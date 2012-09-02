
////////////// warper for pbind
(
SynthDef(\warp, { |out = 0|
	var sig = 0!36;
	var start = 1 + Rand(-0.3, 0.3);
	var dir = \dir.kr(1);
	var light = Hsv2rgb.kr(Rand(0, 1), 1, 1) * EnvGen.kr(Env([0, 1, 0], (1/2.1)!2));
	var pos = start + Phasor.kr(0, 1/ControlRate.ir*\dir.kr(1), 0, 2);
	var panned = MultiPanAz.kr(12, light, pos, 3, -0.5);
	sig = panned.lag(0.3);
	DetectSilence.kr(light, time: 1, doneAction: 2);
	Patcher.all[\waldstock].busesForGroupMethod(\stage, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
}).add;
);

(
var dur = Prand([1, 1, 0.5, 1.5], inf)/Pfunc({~warpdur ?? 1/4});
/*Pdef(\warpaudio, Pbind(\instrument, \warpaudio, \dur, dur));*/
Pdef(\warp, Pbind(\instrument, \warp, \dur, dur, \dir, Pseq([-1, 1], inf)));
/*p = Ppar([ a, b ]);*/
)

/////////////// bckgrnd

m = ProxyChain().addPlayer(\waldstock, \stage, \color, 12, 3);
m.printChain
m.add(\bck, NodeProxy.control(s, 36));
m[\bck].source = {
	var intens = \int.kr(0);
	var trig = Dust.kr(1/3);
	var light = Hsv2rgb.kr(LFSaw.kr(1/23)+1/2 /*+ TRand.kr(0, -0.1, trig)*/, 1, 0.3);
	var out = {Decay.kr(Dust.kr(3 * intens + 0.01), 12)/2 + 0.1 * light}!16;
	out.flatten * \amp.kr(0);
};




////////////////// stage reacts
r = ProxyChain().addPlayer(\waldstock, \stage, \color, 6, 3);
r.printChain
r.add(\react, NodeProxy.control(s, 18));

r[\react].source = {
	var bus1 = Patcher.all[\waldstock].busesForGroupMethod(\ring, \color)[0];
	var bus2 = Patcher.all[\waldstock].busesForGroupMethod(\ring, \color)[15];
	var out1 = { |n|
		DelayL.kr(In.kr(bus1, 3)/3, 0.2, 0.2/3*(2-n));
	}!3;
	var out2 = { |n|
		DelayL.kr(In.kr(bus2, 3)/3, 0.2, 0.2/3*(2-n));
	}!3;
	CombL.kr((out2 ++ out1).flatten, 1.4, 0.4, 2).flatten;
};
