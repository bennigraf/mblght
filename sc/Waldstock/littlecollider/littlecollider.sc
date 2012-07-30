Littlecollider {
	
	var server;
	classvar <>patcher, >channels;
	var particles, <particlebuses, particleaudio;
	var <clock;
	
	var runner;
	
	var bassline;
	var backgroundSynth, audiobackground;
	
	
	*initClass {
		
	}
	
	*new { 
		^super.new.init();
	}
	// new/init: static situation, load all sdefs
	// .start: run action...
	// .reset: reset stuff...
	init {
		server = Server.default;
		particles = List();
		particlebuses = List();
		particleaudio = List();
		patcher = patcher ?? \waldstock;
		channels = channels ?? 20;
		clock = TempoClock.new(128/60);
		
		Routine.run({
			this.loadSdefs();
			server.sync;
			this.createParticles;
			this.createBackground;
		});
	}
	
	loadSdefs {
/*		if(~waldstockpath.isNil, {
			"Define Waldstock path first!".error;
			^false;
		});
		(~waldstockpath++"littlecollider"+/+"sdefs.scd").load;*/
		SynthDef(\bassline, {| amp = 0.51, filter = 0.001, mylag = 4|
			var beat = 128/60;
			var tune = Duty.kr(8/beat, \reset.tr(0), Dseq([36, 30], inf));
			var snd = Pulse.ar(tune.midicps.lag(0.1));
			var fild = Duty.kr(Dseq(beat / [1, 2, 2], inf), \reset.tr(0), Dxrand([2, 2/3, 4, 3, 8, 6/8], inf));
			snd = RLPF.ar(snd, 329 + SinOsc.kr(beat*fild, 0, 55) * filter.lag3(mylag), 0.43);
			snd = HPF.ar(snd, 54);
			Out.ar(0, snd!2 * amp);
		}).add;
		
		SynthDef(\particle, {
			arg speed = 1,
				color = 0,
				resetpos = 0,
				offset = 0,
				trigbus = 100,
				amp = 0;

			var pos = Phasor.kr(Trig1.kr(resetpos), 1/ControlRate.ir * speed, 0, inf) * 2 + offset;
			var trig = Trig1.kr(pos.wrap(-1, 1));
			var width = 3 + EnvGen.kr(Env.perc(0, 0.5, channels-3), trig);
			var light = Hsv2rgb.kr(color, 1, amp);
			var out = MultiPanAz.kr(channels, light, pos.wrap(0, 2), width);

			var sig = out;
			Out.kr(trigbus, trig);
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
		}).add;
		SynthDef(\particledong, { |trigbus = 0, amp = 1, extender = 1|
			var kck, tck;
			var trig = In.kr(trigbus);
			var snd = Saw.ar([36, 40.4]) * EnvGen.kr(Env.perc(0, 0.1), trig);
			snd = RHPF.ar(snd, 73, 0.1);
			kck = LPF.ar(snd, 280);
			tck = BPF.ar(snd, 2000 + {Rand(0, 1000)}!2, 0.1) * EnvGen.kr(Env.perc(0, 0.03, -12.dbamp), trig);
			snd = kck + tck;
			snd = snd + CombC.ar(snd, 0.1, 0.026 + LFNoise1.kr(1/2.83, 0.005), 1, -7.dbamp * extender);
			snd = Compander.ar(snd, snd, 12.dbamp, 1, 1/3, 0.043, 0.83);
			snd = FreeVerb.ar(snd, 1, 0.4, 0.2, -3.dbamp) + snd;
			Out.ar(0, snd * amp);
		}).add;

		SynthDef(\particlebg, { 
			arg amp = 1, 
				speed;
			var sig;
			var trig = Impulse.kr(speed * 8);
			var pos = Demand.kr(trig, 0, Dxrand((0..channels), inf))/channels*2;
			var decayTrigs = PanAz.kr(20, trig!3, pos, level: 1, width: 1, orientation: 0).flop.flatten;
			var light = (Hsv2rgb.kr(4/6, 0.8, 0.5)!20).flatten * amp;
			var lightdecay = EnvGen.kr(Env.perc(0.1, 1.3), decayTrigs);
			sig = light * (1+lightdecay);
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
		}).add;
		SynthDef(\audiobg, { |amp = 1, speed = 0, lag = 0|
			var trig = Impulse.kr(speed * 4);
			var pos = Demand.kr(trig, 0, Dxrand((0..20), inf))/20*2;
			var decayTrigs = PanAz.kr(4, trig, pos, level: 1, width: 1, orientation: 0);
			var lightdecay = EnvGen.kr(Env.perc(0, 0.0161), decayTrigs);
			var tunings = Demand.kr(trig, 0, 
				Dseq([0, 3, 7, 6], inf)
								 + Dwrand([0, 12], [0.9, 0.1], inf)
								 + Dwrand([0, 3, 5], [0.8, 0.1, 0.1], inf);
			);
			var snd = SinOsc.ar((60 + tunings).midicps, mul: 12.dbamp).tanh.softclip;
			snd = snd * lightdecay;
			snd = snd + DelayC.ar(snd, 2, 1/(speed * 4) * (7/4), -9.3.dbamp);
			snd = FreeVerb.ar(snd, 1, 0.53, 0.12) + (snd * LFNoise2.kr(1/6).range(-16, -6).dbamp);
			Out.ar(0, snd * amp.lag3(lag));
		}).add;
		
	}
	
	start {
		
		runner = Routine.run({
			"starting backround".postln;
			backgroundSynth.set(\amp, 0.7, \speed, 1/clock.beatDur);
			16.wait;
			audiobackground.set(\lag, 30, \amp, -12.dbamp, \speed, 1/clock.beatDur);
			48.wait;
/*			16.wait;*/
			// 16 beats are one bassline turnaround
			
			"starting particles".postln;
			this.setKickAmp(-8.dbamp);
			particles.do({|p, n|
				"a particle".postln;
				particlebuses[n].set(1);
				p.set(\amp, 1);
				2.wait;
			});
/*			3.wait;
			2/4.wait;*/
			8.wait;
			
			"speed up particles".postln;
			
			bassline.set(\amp, -26.dbamp, \reset, 1);
			bassline.set(\filter, 1);
			
			this.setParticleSpeed(clock.beatDur*32);
			this.setKickExt(-6.dbamp);
			16.wait;
			64.wait;
			this.setParticleSpeed(clock.beatDur*16);
			this.setKickExt(-12.dbamp);
			32.wait;
			this.setParticleSpeed(clock.beatDur*8);
			this.setKickExt(-18.dbamp);
			24.wait;
			this.setParticleSpeed(clock.beatDur*4);
			this.setKickExt(-24.dbamp);
			8.wait;
			this.setParticleSpeed(clock.beatDur*8);
			8.wait;
			this.setParticleSpeed(clock.beatDur*4);
			this.setKickExt(-44.dbamp);
			8.wait;
			this.setParticleSpeed(clock.beatDur*8);
			8.wait;
			
			// hold for getting straight
			particles.do({ |p|
				p.set(\speed, 0);
			});
			7.wait;
			this.setParticleSpeed(clock.beatDur*8);
			this.setKickExt(0);
			1.wait;
			this.setParticleSpeed(clock.beatDur*4);
			// ab hier normales/schnelles tempo auf die viertel...
			
			2.do({
				// break 1				
				7.wait;
				this.setParticleSpeed(clock.beatDur*1);
				this.setKickAmp(-14.dbamp);
				1.wait;
				this.setKickAmp(-8.dbamp);
				this.setParticleSpeed(clock.beatDur*4);

				// break 2
				6.5.wait;
				this.setParticleSpeed(clock.beatDur*1);
				this.setKickAmp(-14.dbamp);
				0.5.wait;
				this.setKickAmp(-8.dbamp);
				this.setParticleSpeed(clock.beatDur*4);
				0.5.wait;
				this.setParticleSpeed(clock.beatDur*1);
				this.setKickAmp(-14.dbamp);
				0.5.wait;
				this.setKickAmp(-8.dbamp);
				this.setParticleSpeed(clock.beatDur*4);
				
				// break 3
				5.wait;
				this.setParticleSpeed(clock.beatDur*1);
				this.setKickAmp(-14.dbamp);
				2.wait;
				this.setKickAmp(-8.dbamp);
				this.setParticleSpeed(clock.beatDur*4);
				1.wait;
				
				// break 4
				2.do({
					2.5.wait;
					this.setParticleSpeed(clock.beatDur*1);
					this.setKickAmp(-14.dbamp);
					0.5.wait;
					this.setKickAmp(-8.dbamp);
					this.setParticleSpeed(clock.beatDur*4);
					0.5.wait;
					this.setParticleSpeed(clock.beatDur*1);
					this.setKickAmp(-14.dbamp);
					0.5.wait;
					this.setKickAmp(-8.dbamp);
					this.setParticleSpeed(clock.beatDur*4);
				});	
			}); // end of first breaks
			
			// slow stuff down
			16.wait;
			bassline.set(\mylag, 16, \filter, 0.01);
			8.wait;
			this.setParticleSpeed(clock.beatDur*8);
			8.wait;
			this.setParticleSpeed(clock.beatDur*12);
			8.wait;
			this.setParticleSpeed(clock.beatDur*16);
			8.wait;
			this.setParticleSpeed(clock.beatDur*32);
			16.wait;
			
			
			
		}, clock: clock);
	}
	
	reset {
		runner.stop;
		runner.reset;
		Routine.run({
			particles.do({ |p| p.free; });
			particleaudio.do({|p| p.free; });
			server.sync;
			particlebuses.do({|p| p.free; });
			particles = List();
			particlebuses = List();
			particleaudio = List();
			server.sync;
			backgroundSynth.free;
			audiobackground.free;
			bassline.free;
			server.sync;
			
			this.createParticles;
			this.createBackground;
		});
	}
	
	setParticleSpeed { |speed|
		particles.do({ |p|
			p.set(\speed, 1/speed);
		});
	}
	setKickAmp { |amp|
		particleaudio.do({|p|
			p.set(\amp, amp);
		});
	}
	setKickExt { |ext|
		particleaudio.do({|p|
			p.set(\extender, ext);
		});
	}
	
	createParticles {
		4.do({
			particlebuses.add(Bus.control(server, 1));
		});
		server.sync;
		4.do({ |n|
			var synth = Synth(\particledong);
			synth.set(\trigbus, particlebuses[n], \amp, 0);
			particleaudio.add(synth);
		});
		4.do({ |n|
			var synth = Synth(\particle, [\color, n * 0.25, \trigbus, particlebuses[n], \patcher, patcher]);
			var offset = n*0.5+0.25;
			synth.set(\amp, 0, \speed, 0, \offset, offset);
			particles.add(synth);
		});
		server.sync;
	}
	createBackground {
		backgroundSynth = Synth(\particlebg, [\amp, 0]);
		audiobackground = Synth(\audiobg, [\amp, 0]);
		bassline = Synth(\bassline, [\amp, 0]);
	}
}