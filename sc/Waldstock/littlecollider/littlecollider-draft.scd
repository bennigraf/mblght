

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



t = TempoClock(128/60)
t.beats
t.bar
r = Routine({
	7.do({
		"hit".postln;
		1.wait;
	});
})
r.reset
t.play(r)
t.sched(7, r)




///////////////////// audio

(SynthDef(\audiobg, { |amp = 1, speed = 0|
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
	Out.ar(0, snd * amp);
}).add;)
x.free
x = Synth(\audiobg, [\speed, 128/60]);
x = Synth(\audiobg, [\speed, 1/4]);



//////////////////// bass
~waldstockpath
~trigbus = Bus.control(s, 1)
(
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
)
x = Synth(\particledong, [\trigbus, ~trigbus, \amp, -12.dbamp]);
~trigbus.scope
x.free
y = {
	Out.kr(~trigbus, Impulse.kr(1/3))
}.play
y.free

y = x.setMsg(\amp, 0.2)
y

(
SynthDef(\bsss, { |amp = 1, filter = 0.001, mylag = 4|
	var beat = 128/60;
	var tune = Duty.kr(8/beat, \reset.tr(0), Dseq([36, 30], inf));
	var snd = Pulse.ar(tune.midicps.lag(0.1));
	var fild = Duty.kr(Dseq(beat / [1, 2, 2], inf), \reset.tr(0), Dxrand([2, 2/3, 4, 3, 8, 6/8], inf));
	snd = RLPF.ar(snd, 329 + SinOsc.kr(beat*fild, 0, 55) * filter.lag3(mylag), 0.43);
	snd = HPF.ar(snd, 54);
	Out.ar(0, snd!2 * amp);
}).add;
)
y.free
y = Synth(\bsss, [\amp, -20.dbamp]);
y.set(\reset, 1)
y.set(\mylag, 4)
y.set(\filter, 1)
