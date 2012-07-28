

// 4 (?) objects running around in a circle, everytime they hit the stage they emit a light.
// first: very slow, synchronously, then with an offset (so that they meet/overlap), then really fast
// until climax, short interlude, then get slower again with offset, ultimately stop.

~trigbus = Bus.control(s, 1)
(
SynthDef(\particle, { |speed = 1, color = 0, resetpos = 0, offset = 0, id = 0|
/*	var phase = Phasor.ar(Trig1.kr(resetpos), 1/SampleRate.ir, 0, inf);*/
	var pos = LFSaw.kr(speed * MouseY.kr(1, 10), offset); // to go from 0 to 2
	var trig = Trig.kr(pos);
	var width = 3 + Decay.kr(trig, 0.5, mul: 8);
	var light = Hsv2rgb.kr(color, 1, 1);
	var out = MultiPanAz.kr(20, light, pos, width);
	
/*	var trig = Out.kr(~trigbus, Trig.kr(pos.poll).poll);*/
	
	
	
	var sig = out;
	Patcher.all[\waldstock].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
	SendTrig.kr(trig, Rand(60, 80));
}).add;
)
x.free; y.free; z.free; a.free;
x = Synth(\particle);

y.set(\speed, 0.65)

b = Synth(\particle, [\offset, 0.25, \speed, 0]);
y = Synth(\particle, [\offset, 0.75, \speed, 0]);
z = Synth(\particle, [\offset, 1.25, \speed, 0]);
a = Synth(\particle, [\offset, 1.75, \speed, 0]);
b.set(\color, 0.25); y.set(\color, 0.5); z.set(\color, 0.75)

~base = 0.051
[b, y, z, a].do({ |e, i|
	e.set(\speed, ~base * ((i+1)/3 + 1))
/*	e.set(\speed, 0)*/
})
4.nthPrime


Ndef.audio(\test, 2)
Ndef(\test, {
	HPF.ar(SinOsc.ar(mul: 4).tanh!2, 1000 + LFNoise1.kr(18).range(0, 200), 0.1) * EnvGen.kr(Env.perc(0, 0.021), In.kr(~trigbus))
})
Ndef(\test).play
OSCFunc.trace(false)
OSCdef(\trig, { |msg|
	(dur: 0.1, note: msg[2]-50).play;
}, '/tr')