s.options.device_("JackRouter")
ServerOptions.devices


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
	var width = 3 + Decay.kr(trig, 0.5, mul: 8).poll;
	var light = Hsv2rgb.kr(color, 1, 1) * amp;
	var out = MultiPanAz.kr(20, light, pos.wrap(0, 2), width);
	
	var sig = out;
	Patcher.all[\waldstock].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
/*	SendTrig.kr(trig, Rand(60, 80));*/
	Out.kr(trigbus, trig);
}).add;

x = Synth(\particle, [\amp, 0])
x.set(\amp, 1)
x.set(\amp, 0)

(SynthDef(\particlebg, { 
	arg amp = 1, 
		speed = 0;
	var patcher = \waldstock;
	var channels = 20;
	var sig;
	var trig = Impulse.kr((speed * 4));
	var pos = Demand.kr(trig, 0, Dxrand((0..channels), inf))/channels*2;
	var decayTrigs = PanAz.kr(20, trig!3, pos, level: 1, width: 1, orientation: 0).flop.flatten;
	var light = (Hsv2rgb.kr(4/6, 0.8, 0.5)!20).flatten * amp;
	var lightdecay = EnvGen.kr(Env.perc(0.5, 0.8), decayTrigs);
	sig = light * (1+lightdecay);
	Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
}).add;
)
x.free
x = Synth(\particlebg, [\amp, 0.4]);
x.set(\amp, 1)
x.set(\speed, 128/6)