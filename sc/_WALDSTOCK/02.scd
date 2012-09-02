LGui();
(
MIDIClient.init;
//There are three possibilities to connect for example to the first device:
/*MIDIIn.connect(0, -1101621857);*/
//MIDIIn.connect(0, MIDIClient.sources.at(0).uid);
MIDIIn.connectAll;
~mout = MIDIOut(3);
/*~mout = MIDIOut.newByName('LPD8')*/

)
/*~mout.noteOn(0, 8)*/
/*~mout.noteOff(0, 8)*/


Routine.run({
	// white
	Ndef(\node1).kr();
	Ndef(\node1).source_({
		var buses = Patcher.default.busesForMethod(\dim);
		buses.do({ |bus|
			Out.kr(bus, (\amp.kr(0) * \sw.kr(0)));
		});
		0;
	});
	s.sync;
	// col
	Ndef(\node2).kr();
	Ndef(\node2).source_({
		var buses = Patcher.default.busesForGroupMethod(\stage, \color);
		var ahue = \hue.kr(0) + LFSaw.kr(\colorwheel.kr(0).lincurve(0, 1, 0, 1, 3).lag2(2), 0.5, 0.5, 0.5);
		var delays = 0.5-((0..19)/20-0.5).abs;
		var hue = DelayL.kr(ahue, 0.5, delays);
/*		var color = Hsv2rgb.kr(hue.wrap(0, 1), \sat.kr(1), 0.5);//.lag(0.5);*/
		var color = { |n|
			hue[n].postln;
			Hsv2rgb.kr(hue[n].wrap(0, 1), \sat.kr(1), 1);
		}!buses.size;
		var intens = \int.kr(0)!3;
		var unruhe = { Decay.kr(Dust.kr(3 * intens + 0.01), 2)/2 }!buses.size;
		var sig = (color * \amp.kr(0) * (1 - intens)) + (intens * color * unruhe);
/*		var sig = color;*/
/*		delays.poll(2);*/
/*		color[0].size.postln;*/
/*		sig = sig.flop.flatten;*/
		sig = sig * \sw.kr(0) * \amp.kr(0);
		buses.do({ |bus, i|
			Out.kr(bus, [sig[i][0], sig[i][1], sig[i][2]]);
		});
		0;
	});
	Ndef(\node2).source_(nil)

	// strobo
	Ndef(\node3).kr();
	Ndef(\node3).source_({
		var strobe = \amp.kr(0) + LFNoise0.kr(8).range(0, 1 * (1 - \amp.kr(0))) * \sw.kr(0);
		var buses = Patcher.default.busesForMethod(\blitz);
		buses.do({ |bus, i|
			Out.kr(bus, strobe);
		});
		0;
	});
	
	// fog Node4, but direct bus access
	
	// warpding
	Ndef(\node5).kr();
	Ndef(\node5).source_({
		var buses = Patcher.default.busesForGroupMethod(\stage, \color);
		var sig = 0!(buses.size*3);
		var start = TRand.kr(-0.3, 0.3, \trig.tr(0));
		var dir = Demand.kr(\trig.tr(0), 0, Dseq([-1, 1], inf));
		var light = Hsv2rgb.kr(TIRand.kr(0, 1, \trig.tr(0)), 1, 1) * LagUD.kr(Trig1.kr(\trig.tr(0), 1/2), 1/2, 1/2);
		var pos = start + Phasor.kr(0, 1/ControlRate.ir*dir, 0, 2);
		var panned = MultiPanAz.kr(buses.size, light, pos.wrap(0, 2), 3, -0.5);
		sig = panned.lag(0.3);
/*		DetectSilence.kr(light, time: 1, doneAction: 2);*/
		Patcher.default.busesForGroupMethod(\stage, \color).do({ |bus, i|
			Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
		});
		0;
	});

	Ndef(\node6).kr().source_({
		
	});
	Ndef(\node7).kr().source_({
		
	});
	Ndef(\node8).kr();
	Ndef(\node8).source_({
		var buses = Patcher.default.busesForGroupMethod(\stage, \color);
		var sig = ({|n| Hsv2rgb.kr((SinOsc.kr(1/SinOsc.kr(1/4).range(2, 56), pi/16*n)+1/2), 1, 1); }!buses.size).flatten;
		var pos = LFSaw.kr(1/SinOsc.kr(1/3.8).range(1, 8), 0, 1, 1);
		var width = EnvGen.kr(Env.perc(0, 2, 1, 0), Dust.kr(1/2 * \int.kr(0).linlin(0, 1, 1/2, 8))) * 7 + 2;
		var rate = width + 6;
		sig = \sw.kr(0) * sig * Blitzen.kr(buses.size, 3, rate: rate, pos: pos, width: width, ori: -0.5).clip;
		buses.do({ |bus, i|
			Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
		});
		0;
	});
	
	// white
	MIDIdef.noteOn(\node1, { |msg|
		if(Ndef(\node1).get(\sw) == 1, {
			Ndef(\node1).set(\sw, 0);
			~mout.noteOff(0, 1);
		}, {
			Ndef(\node1).set(\sw, 1);
			~mout.noteOn(0, 1);
		});
	}, 1);
	MIDIdef.cc(\node1a, { |val|
		Ndef(\node1).set(\amp, val/127);
	}, 1);
	
	// color node
	MIDIdef.noteOn(\node2, { |msg|
		if(Ndef(\node2).get(\sw) == 1, {
			Ndef(\node2).set(\sw, 0);
			~mout.noteOff(0, 2);
		}, {
			Ndef(\node2).set(\sw, 1);
			~mout.noteOn(0, 2);
		});
	}, 2);
	MIDIdef.cc(\node2a, { |val|
		Ndef(\node2).set(\amp, val/127);
	}, 2);
	MIDIdef.cc(\node2b, { |val|
		Ndef(\node2).set(\hue, val/127);
	}, 5);
	MIDIdef.cc(\node2c, { |val|
		Ndef(\node2).set(\sat, val/127);
	}, 6);
	MIDIdef.cc(\node2d, { |val|
		Ndef(\node2).set(\int, val/127);
	}, 7);
	
	// strobo
	MIDIdef.noteOn(\node3, { |msg|
		if(Ndef(\node3).get(\sw) == 1, {
			Ndef(\node3).set(\sw, 0);
			~mout.noteOff(0, 3);
		}, {
			Ndef(\node3).set(\sw, 1);
			~mout.noteOn(0, 3);
		});
	}, 3);
	MIDIdef.cc(\node3a, { |val|
		Ndef(\node3).set(\amp, val/127);
	}, 3);
	
	
	// fog
	MIDIdef.noteOn(\node4, { |msg|
		if(Patcher.default.busesForMethod(\fog)[0].getSynchronous > 0, {
			Patcher.default.busesForMethod(\fog)[0].set(0);
			~mout.noteOff(0, 4);
		}, {
			Patcher.default.busesForMethod(\fog)[0].set(1);
			~mout.noteOn(0, 4);
		});
	}, 4);
	
	MIDIdef.noteOn(\node5, {
		Ndef(\node5).set(\trig, 1);
	}, 5);
	
	// colorsw
	MIDIdef.noteOn(\node6, { |msg|
		msg.postln;
		if(Ndef(\node2).get(\colorwheel) > 0, {
			Ndef(\node2).set(\colorwheel, 0);
			~mout.noteOff(0, 6);
		}, {
			Ndef(\node2).set(\colorwheel, msg.linlin(30, 127, 0, 1));
			~mout.noteOn(0, 6);
		});
	}, 6);
	
	// blitzen
	MIDIdef.noteOn(\node8, { |msg|
		if(Ndef(\node8).get(\sw) == 1, {
			Ndef(\node8).set(\sw, 0);
			~mout.noteOff(0, 8);
		}, {
			Ndef(\node8).set(\sw, 1);
			~mout.noteOn(0, 8);
		});
	}, 8);
	MIDIdef.cc(\node8a, { |val|
		Ndef(\node8).set(\int, val/127);
	}, 8);
})
~
