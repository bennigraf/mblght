Littlecollider {
	
	var server;
	classvar <>patcher, >channels;
	var particles, particlebuses;
	var clock;
	
	var backgroundSynth;
	
	
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
				amp = 1,
				patcher = \waldstock;

			var pos = Phasor.ar(Trig1.kr(resetpos), 1/SampleRate.ir*2*speed, 0, inf) + offset;
			var trig = Trig.kr(pos.wrap(-1, 1));
			var width = 3 + Decay.kr(trig, 0.5, mul: 8);
			var light = Hsv2rgb.kr(color, 1, 1) * amp;
			var out = MultiPanAz.kr(channels, light, pos.wrap(0, 2), width);

			var sig = out;
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
			Out.kr(trigbus, trig);
		}).add;

		SynthDef(\particlebg, { |amp = 1, patcher = \waldstock|
			var sig;
			var trig = Impulse.kr(8.3);
			var decay = Decay.kr(trig, 0.3)!3;
			var pos = TIRand.kr(0, channels-1, trig)/channels*2;
			var decayTrigs = PanAz.kr(20, trig!3, pos, level: 1, width: 1, orientation: 0);
			var light = (Hsv2rgb.kr(5/6, 0.8, 0.5)!20).flatten * amp;
/*			var lightdecay = MultiPanAz.kr(channels, decay, pos, 1, ori: 0);*/
			var lightdecay = EnvGen.kr(Env.perc(0.1, 0.5), decayTrigs);
			sig = light * (1+lightdecay);
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
		}).add;

	}
	
	createParticles {
		4.do({
			particlebuses.add(Bus.control(server, 1));
		});
		server.sync;
		4.do({ |n|
			var synth = Synth(\particle, [\offset, n * 0.5 + 0.25, 
											\color, n * 0.25, 
											\trigbus, particlebuses[n];
											\speed, 0,
											\amp, 0,
											\patcher, patcher]);
			particles.add(synth);
		});
		server.sync;
	}
	createBackground {
		backgroundSynth = Synth(\particlebg, [\amp, 0]);
	}
}