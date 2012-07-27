TouchLFO {
	
	var id; // int id
	var <controls; // touchcontrols...
	var server;
	classvar <types, <typeIndexes;
	classvar <globalPhasor; // holds global phasor-thing? Maybe a NodeProxy that can be used with a .kr method?
	classvar <globalBpm;
	
	var <myPhasor; // local phasor of this lfo (for offset & own duration)
	var <lfo; // nodeproxy with lfo
	var <bus; // bus the lfo writes to
	var type; // type of lfo...
	var wavetable; // Signal (float array) with values from wavetable
	var <wavetableBuf;
	var traceNode; // Node on server to get lfo data from server
	var traceResp; // OSCFunc to send data from server to touchosc with
	
	*initClass {
		typeIndexes = ['sine', 'saw', 'pulse', 'impulse', 'dust', 'lfnoise', 'lfnoiselin', 'waveform'];
/*		types = Event(8);*/
		types = ();
		types.add(\sine -> (
				shaper: { |phas| 
					{ SinOsc.kr(0, In.kr(phas.bus) * 2pi, 0.5, 0.5); }
				},
				wavetable: { Signal.sineFill(20, [1]) * 0.5 + 0.5; }
			));
		types.add(\saw -> (
				shaper: { |phas| 
					{ In.kr(phas.bus); }
				},
				wavetable: { Signal.fill(20, 0).waveFill({|x| x}, 0, 1) }
			));
		types.add(\pulse -> (
				shaper: { |phas| 
					{ (1-In.kr(phas.bus)).round(1); }
				},
				wavetable: { Signal.fill(20, 0).waveFill({|x| (1-x-0.01).round}, 0, 1) }
			));
		types.add(\impulse -> (
				shaper: { |phas|
					{ Trig1.kr(In.kr(phas.bus)-0.001, 1/20); }
				},
				wavetable: { Signal.fill(20, 0)[0] = 1 }
			));
		types.add(\dust -> (
				shaper: { |phas| 
					{ Trig.kr(Dust.kr(Slope.kr(In.kr(phas.bus)) * 2), 1/20); } 
				},
				wavetable: { Signal.fill(20, 0) }
			));
		types.add(\lfnoise -> (
				shaper: { |phas| 
					{ var freq = (Slope.kr(In.kr(phas.bus)) * 4).clip(1, 10); LFNoise0.kr(freq, 0.5, 0.5); } 
				},
				wavetable: { Signal.fill(20, 0) }
			));
		types.add(\lfnoiselin -> (
				shaper: { |phas| 
					{ var freq = (Slope.kr(In.kr(phas.bus)) * 4).clip(1, 10); LFNoise1.kr(freq, 0.5, 0.5); } 
				},
				wavetable: { Signal.fill(20, 0) }
			));
		types.add(\waveform -> (
				shaper: { |phas| 
					{ BufRd.kr(1, \wtBuf.kr(0), In.kr(phas.bus) * 20, 1, 2); }
				},
				wavetable: { Signal.fill(20, 0) }
			));
	}
	
	*new { |myId, myType = nil|
		^super.new.init(myId, myType);
	}
	
	init { |myId, myType = nil|
		id = myId;
		type = myType ?? \sine;
		server = Server.default;
		server.waitForBoot({
			server.sync;
			wavetableBuf = Buffer.alloc(server, 20, 1);
			server.sync;
			this.initControls;
			server.sync;
			if(globalPhasor.isNil, {
				this.initGlobalPhasor;
				server.sync;
			});
			this.initPhasor;
			server.sync;
			bus = Bus.control(server, 1);
			server.sync;
			this.initLFO;
			server.sync;
			this.initLFOResponder;
			this.setType(type);
			server.sync;
		});
	}
	
	initGlobalPhasor {
		Routine.run({
			var bpmbutton, bpmlabel, globphase, bpmreset;
			globalBpm = 120; // 120bpm...
			globphase = TouchControl(\fader, '/globphase');
			while({globphase.bus.isNil}, { 0.01.wait; });
			globalPhasor = NodeProxy.control(server, 1).source_({
				// 2 sec are 1 period/1second
				var phase = Phasor.kr(\resetTrig.tr(0), 1/ControlRate.ir/2 * \tempo.kr(1), 0, inf, 0);
				Out.kr(globphase.bus, phase.wrap(0, 1));
				phase;
			});
			bpmbutton = TouchControl(\button, '/taptempo', false);
			bpmlabel = TouchControl(\label, '/bpmlabel', false);
			bpmreset = TouchControl(\button, '/bpmreset', false);
			bpmreset.action_({ |val| if(val==1, {globalPhasor.set(\resetTrig, 1);}) });
			bpmlabel.set("120");
			
			server.sync;
			bpmbutton.action_({
				var lasthits = [];
				{ |val|
					if(val == 1, {
						var time = thisThread.seconds;
						var hits = [];
						hits = lasthits.select({|item| (time - item) < 8 });
						if(hits.size == 4, {
							var deltas = [];
							var tempo; // sekunden pro hit
							3.do({ |n| deltas = deltas.add(hits[n+1] - hits[n]); });
							tempo = deltas.mean; // durchschnitt
							globalBpm = 1 / tempo * 60;
							bpmlabel.set((globalBpm.round(0.1)).asString);
							globalPhasor.set(\tempo, globalBpm/120);
						});
						if(lasthits.size == 4, {
							lasthits.removeAt(0);
						});
						lasthits = lasthits.add(time);
					});
				}
			}.value());
		});
	}
	initPhasor {
		myPhasor = NodeProxy.control(server, 1).source_({
			var globPhas = globalPhasor.kr;
			// multiply with 'dur' -> 1/4, 1/2, 1, 2, 4, ...
			// phas = phas * In.kr(controls.dur.bus);
			var phas = globPhas * Latch.kr(\dur.kr(1), Trig.kr(Impulse.kr(0) + Changed.kr(\dur.kr(1)), 0.2));
			// offset phase by up to 1 period = 2 second
			phas = phas + In.kr(controls.phaseoffset.bus);
			// finally wrap around 1, 
			phas = phas.wrap(0, 1);
			// play out to TouchControl
			Out.kr(controls.phase.bus, phas);
			// play out to NodeProxy-Bus
			phas;
		});
	}
	
	initControls {
		controls = ();
		controls.type = TouchControl(\button, '/lfo'++id++'/type', false);
		controls.typeLabel = TouchControl(\label, '/lfo'++id++'/typelabel', false);
		controls.dur = TouchControl(\fader, '/lfo'++id++'/dur', false);
		controls.durlabel = TouchControl(\label, '/lfo'++id++'/durlabel', false);
		controls.durlock = TouchControl(\toggle, '/lfo'++id++'/durlock', false);
		controls.phaseoffset = TouchControl(\fader, '/lfo'++id++'/phaseoffset');
		controls.phase = TouchControl(\fader, '/lfo'++id++'/phase');
		controls.resetphase = TouchControl(\button, '/lfo'++id++'/resetphase', false);
		controls.waveform = TouchControl(\multifader, '/lfo'++id++'/waveform', false, 20);
		controls.amp = TouchControl(\fader, '/lfo'++id++'/amp');
		server.sync;
		
		// switch types, set wavetable accordingly
		controls.type.action_({ |val|
			if(val == 1, {
				this.setType;
			});
		});
		
		// default speed: 1
		controls.durlock.set(1);
		controls.dur.set(0.5); // fader...
		controls.durlabel.set("1");
		// dur modulator magic
		controls.dur.action_({ |val|
			var myVal, setStr;
			if(controls.durlock.val == 1, {
				var grid = [0, 1/4, 1/3, 1/2, 1, 2, 3, 4, 8];
				myVal = (val * 8).round;
				myVal = grid[myVal];
				if(myVal<1, {
					var frac = myVal.asFraction;
					setStr = frac[0].asString++"/"++frac[1].asString;
				}, {
					setStr = myVal.asString;
				});
			}, {
				myVal = val.lincurve(0, 1, 0, 8, 4);
				setStr = myVal.round(0.01).asString;
			});
			controls.durlabel.set(setStr);
			myPhasor.set(\dur, myVal);
		});
		
		// wavetable synth!!!
		// on wavetable action jump to Wavetable type
		controls.waveform.action_({ |index, val|
			if(index.size == 0 && val.notNil, {
				if(type != \waveform, {
					this.setType(\waveform);
				});
				wavetable[index] = val;
				wavetableBuf.set(index, val);
			});
		});
		
		// magic amp!
		// doens't work yet, must implement algorightm in player...
		controls.amp.action_({ |val|
			var myWav;
			if(type!=\waveform, {
				this.setType(\waveform);
			});
			lfo.set(\magicAmp, val);
			val = val * 4 + 1;
			myWav = wavetable.as(Array) * 2 - 1;
			myWav = myWav.abs**(1/val) * myWav.sign /2 + 0.5;
			controls.waveform.set(myWav);
		});
		
		
	}
	
	setType { |aType = nil|
		if(aType == nil, { 
			var index = typeIndexes.find([type]);
			type = typeIndexes.wrapAt(index + 1);
		}, {
			type = aType;
		});
		controls.typeLabel.set(this.typeAsUppercasedString);
		Routine.run({
			lfo[0] = types[type][\shaper].value(myPhasor);
			if(type == \waveform, {
				lfo.set(\wtBuf, wavetableBuf.bufnum);
			});
		});
		if(type!=\waveform, {
			this.setWaveTable;
		});
	}
	
	initLFO {
		lfo = NodeProxy.control(server, 1);
		lfo[0] = types[type][\shaper].value(myPhasor);
		lfo[1] = \filter -> { |in|
			var val = \magicAmp.kr(0) * 4 + 1;
			in = in * 2 - 1;
			in = in.abs**(1/val) * in.sign / 2 + 0.5;
		};
		lfo.set(\wtBuf, wavetableBuf.bufnum);
	}
		
	initLFOResponder {
		traceResp = OSCFunc({ |msg|
			TouchControl.replAddr.sendMsg('/lfo'++id, msg[3]);
		}, '/lfo'++id, TouchControl.recvAddr);
		traceNode = NodeProxy.control(server, 1).source_({
			SendReply.kr(Impulse.kr(25), '/lfo'++id, In.kr(lfo.bus));
		});
	}
	
	setWaveTable {
		wavetable = types[type][\wavetable].value();
		controls.waveform.set(wavetable.as(Array));
		wavetableBuf.sendCollection(wavetable.as(Array));
	}
	
	typeAsUppercasedString {
		var myType = type.asString;
		myType[0] = myType[0].toUpper;
		^myType;
	}
}
