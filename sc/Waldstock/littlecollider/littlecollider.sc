Littlecollider {
	
	var server;
	classvar <>patcher, >channels;
	var particles, particlebuses;
	var <clock;
	
	var runner;
	
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
		SynthDef(\particle, {
			arg speed = 1,
				color = 0,
				resetpos = 0,
				offset = 0,
				trigbus = 0,
				amp = 0;

			var pos = Phasor.kr(Trig1.kr(resetpos), 1/ControlRate.ir*2*speed, 0, inf) + offset;
			var trig = Trig1.kr(pos.wrap(-1, 1));
			var width = 3 + EnvGen.kr(Env.perc(0, 0.5, channels-3), trig);
/*			var width = 3;*/
			var light = Hsv2rgb.kr(color, 1, amp);
/*			var out = MultiPanAz.kr(channels, light, pos.wrap(0, 2), width);*/
			var out = MultiPanAz.kr(channels, light, pos.wrap(0, 2), width);

			var sig = out;
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
			Out.kr(trigbus, trig * amp);
		}).add;

		SynthDef(\particlebg, { 
			arg amp = 1, 
				speed;
			var sig;
			var trig = Impulse.kr(speed * 8);
			var pos = Demand.kr(trig, 0, Dxrand((0..channels), inf))/channels*2;
			var decayTrigs = PanAz.kr(20, trig!3, pos, level: 1, width: 1, orientation: 0).flop.flatten;
			var light = (Hsv2rgb.kr(4/6, 0.8, 0.5)!20).flatten * amp;
			var lightdecay = EnvGen.kr(Env.perc(0.5, 0.8), decayTrigs);
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
			Out.ar(0, snd * amp.lag(lag));
		}).add;
		
	}
	
	start {
		
		runner = Routine.run({
			"starting backround".postln;
			backgroundSynth.set(\amp, 0.7, \speed, 1/clock.beatDur);
			audiobackground.set(\lag, 30, \amp, -12.dbamp, \speed, 1/clock.beatDur);
			64.wait;
			
			"starting particles".postln;
			particles.do({|p|
				"a particle".postln;
				p.set(\amp, 1);
				p.get(\offset, { arg value; value.postln; });
				1.wait;
			});
			3.wait;
			2/4.wait;
			
			"speed up particles".postln;
			particles.do({ |p|
				p.set(\speed, clock.beatDur/4);
			});
			
		}, clock: clock);
	}
	
	reset {
		runner.stop;
		runner.reset;
		Routine.run({
			particles.do({ |p|
				p.free;
			});
			server.sync;
			particlebuses.do({|p|
				p.free;
			});
			particles = List();
			particlebuses = List();
			server.sync;
			backgroundSynth.free;
			audiobackground.free;
			server.sync;
			
			this.createParticles;
			this.createBackground;
		});
	}
	
	createParticles {
		4.do({
			particlebuses.add(Bus.control(server, 1));
		});
		server.sync;
		4.do({ |n|
			var synth = Synth(\particle, [	\color, n * 0.25, 
											\trigbus, particlebuses[n];
											\patcher, patcher]);
			var offset = n*0.5+0.25;
			offset.postln;
			synth.set(\amp, 0, \speed, 0, \offset, offset);
			particles.add(synth);
		});
		server.sync;
	}
	createBackground {
		backgroundSynth = Synth(\particlebg, [\amp, 0]);
		audiobackground = Synth(\audiobg, [\amp, 0]);
	}
}