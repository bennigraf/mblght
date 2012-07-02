(
SynthDef(\roar, { 
	arg startPos = 0,
		endPos = 0,
		myDur = 0,
		myFact = 0,
		trig = 0;
		
	var myPos = Sweep.kr(trig, 1/myDur) * endPos;
	var myWidth = 1 + (Sweep.kr(trig, 1/myDur) * 3);
	var d = 0.15;
	var a = myFact.linlin(1, 10, myDur/2, 0.1);
	var r = myFact.linlin(1, 10, myDur/2, myDur) - 0.1 -d;
	var env = EnvGen.kr(Env.perc(a, r), TDelay.kr(trig, d)).lincurve(0, 1, 0, 1, 1);
	
	var panned, mirror;
	var outs = 0!4;
	
	var fact = myFact.linlin(1, 10, 0, 1);
	var snd = SinOsc.ar(SinOsc.kr(1/13, 2pi.rand, 0.5, 0.5).linexp(0.01, 1, 200, 240)) * Pulse.ar(20+LFNoise0.ar(20, 4));
	snd = snd * Pulse.ar(12.42 + (5 * fact));
	snd = RLPF.ar(snd, 380-(1-fact * 180), 0.1);
	snd = (snd*(((1-fact * -12)-3).dbamp)).softclip;
	snd = LPF.ar(snd, 1000);
	snd = Compander.ar(snd, snd, -22.dbamp, 1, 1/4);
	snd = HPF.ar(snd, 80) * env;
	snd = DelayL.ar(snd, 0.1, 0.1);
	
/*	panned = PanAz.ar(4, snd, myPos, 1, 2, 2.5);*/
	panned = PanAz.ar(4, snd, myPos, 1, 2, 0.5);
	mirror = PanAz.ar(4, snd, myPos.neg, 1, 2, 0.5);
/*	4.do{ |n|
		outs[n] = panned[n] + DelayL.ar(panned[3-n], 1, 1);
	};*/
	mirror = DelayL.ar(mirror, 0.2, 0.2 - (fact * 0.1));
	mirror = BPF.ar(mirror, 1200, 0.3, 2);
	outs = panned + (mirror * -3.dbamp);
	
	DetectSilence.ar(outs.sum, doneAction: 2);
	Out.ar(0, outs);
}).add;
)
Synth(\roar, [\endPos, 0.9, \myDur, 1, \myFact, 1, \trig, 1], t);
Synth(\roar, [\endPos, 1, \myDur, 4, \myFact, 1, \trig, 1], t);

(
x.free;
x = {
	var panned;
	var fact = LFSaw.kr(1/8, 1).range(0, 1);
	var snd = SinOsc.ar(SinOsc.kr(1.3, 0, 0.5, 0.5).linexp(0.01, 1, 200, 240)) * Pulse.ar(20+LFNoise0.ar(20, 4));
	snd = RLPF.ar(snd, 380-(1-fact * 120), 0.1);
	snd = (snd*(((1-fact * -12)-3).dbamp)).softclip;
	snd = LPF.ar(snd, 1000);
	snd = Compander.ar(snd, snd, -18.dbamp, 1, 1/4);
/*	snd!2;*/
	panned = PanAz.ar(4, snd, LFSaw.kr(1/6, 1).range(0, 2), MouseY.kr(0, 1), 2);
	panned;
}.play(t);)

t=Server.internal

ServerOptions.devices
t.options.device_("Agg")
t.boot
t.quit

t.recChannels_(4);
t.recSampleFormat_("int16");
t.record
t.stopRecording
