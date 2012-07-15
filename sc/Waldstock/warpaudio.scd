(
SynthDef(\warpaudio, { |out=0|
	var snd = Saw.ar({Rand(200, 280)/5}) * {LFNoise1.kr(3) * 0.2 + 0.7}!7;
	snd = snd * (1-Pulse.ar(13, 0.5, 0.7));
	snd = LPF.ar(snd.sum, 250);
	snd = FreeVerb.ar(snd, 0.4, 1, 0);
	snd = snd * EnvGen.ar(Env.perc(0.8, 2.4)) * 7.dbamp;
	snd = LPF.ar(snd.softclip, 839);
	DetectSilence.ar(snd, doneAction:2);
	Out.ar(0, snd!2 * -17.dbamp);
}).add;
);


Ndef(\bck, {
	var trig = Impulse.kr(1/6.3);
	var snd = PinkNoise.ar * {LFNoise1.kr(3) * 0.2 + 0.7}!7;
	var intens = \int.kr(0);
	snd = snd + BPF.ar(({Decay.ar(Dust.ar(3 * intens + 0.01), 0.2) * (18.9 * intens + 2)}!7).sum, TRand.kr(400, 847, trig), 0.1);
	snd = FreeVerb.ar(snd, 0.4, 1, 0);
	snd = BPF.ar(snd, {TRand.kr(700, 1000, trig)}!4, 0.01).sum;
	snd = snd * 0.dbamp;
});
/*Ndef(\bck).play*/
