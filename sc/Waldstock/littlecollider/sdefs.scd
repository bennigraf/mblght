
SynthDef(\particle, {
	arg speed = 1,
		color = 0,
		resetpos = 0,
		offset = 0,
		trigbus = 0,
		amp = 1,
		patcher = \waldstock;
		
/*	var phase = Phasor.ar(Trig1.kr(resetpos), 1/SampleRate.ir, 0, inf);*/
/*	var pos = LFSaw.kr(speed * MouseY.kr(1, 10), offset); // to go from 0 to 2*/
	var pos = Phasor.ar(Trig1.kr(resetpos), 1/SampleRate.ir*2*speed, 0, inf) + offset;
	var trig = Trig.kr(pos.wrap(-1, 1));
	var width = 3 + Decay.kr(trig, 0.5, mul: 8);
	var light = Hsv2rgb.kr(color, 1, 1) * amp;
	var out = MultiPanAz.kr(20, light, pos.wrap(0, 2), width);
	
	var sig = out;
	Patcher.all[\waldstock].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
/*	SendTrig.kr(trig, Rand(60, 80));*/
	Out.kr(trigbus, trig);
}).add;

(SynthDef(\particlebg, { |amp = 1|
	var patcher = \waldstock;
	var channels = 20;
	var sig;
	var trig = Impulse.kr(8.3);
	var decay = Decay.kr(trig, 0.3)!3;
	var pos = TIRand.kr(0, channels-1, trig)/channels*2;
	
	var light = (Hsv2rgb.kr(5/6, 0.8, 0.5)!20).flatten * amp;
	
	var lightdecay = MultiPanAz.kr(channels, decay, pos, 1, ori: 0);
	sig = light * (1+lightdecay);

	Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});

}).add;
)
x.free
x = Synth(\particlebg, [\amp, 1]);
x.set(\amp, 0.4)