s.options.device_(ServerOptions.devices[0]) // mackie
s.options.device_("8chan")
s.options.device_("JackRouter")
s.quit
s.boot
g = RootNode(s)


~buf = Buffer.read(s, "/Users/bennigraf/Desktop/12 Shine.wav")
~buf1 = Buffer.readChannel(s, "/Users/bennigraf/Desktop/12 Shine.wav", 0, -1, [0])
~buf2 = 0!2
~buf2[0] = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/shine-intro.aif", 0, -1, 0)
~buf2[1] = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/shine-intro.aif", 0, -1, 1)
~buf2
x = ~buf1.play
x.free
b = Buffer.read(s, Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff");

Ndef.control(\phase, 1)
Ndef(\phase).source = {
/*	LFSaw.kr(1/(~buf2[0].numFrames/~buf2[0].sampleRate), 0, 0.5, 0.5);*/
	Phasor.kr(0, 1/ControlRate.ir/(~buf2[0].numFrames/~buf2[0].sampleRate), 0, inf)
}
Ndef.audio(\shine1, 4)
Ndef(\shine1).play
(Ndef(\shine1).source = {
/*	BufRd.ar(2, ~buf, K2A.ar(MouseX.kr(0, ~buf.numFrames/4)).lag(2), 4);*/
/*	GrainBuf.ar(2, Dust.kr(20), 0.01, ~buf1, MouseX.kr(0, 1/10))*/
	var trig = Impulse.kr(LFNoise1.kr(3).range(8, 40) * 10) + Dust.kr(18);
/*	var snd = GrainBuf.ar(2, trig, TRand.kr(0.031, 0.16, trig), ~buf2, 0.2,
	            Ndef(\phase).kr + TRand.kr(0.0001, 0.001, trig), 2, 0, -1, 512, 2).sum;*/
/*	var rate = EnvGen.kr(Env([0.2, 0.2, 0.9], [20, 20]), 1);*/
/*	var phase = Ndef(\phase).kr/Line.kr(8, 1, 20) * ~buf2[0].duration * TRand.ar(1.0, 1.001, trig);*/
	var phase = 2.27327 * TRand.ar(1.0, 1.15, trig);
	var rate = 0.13;
	var windowsize = TRand.ar(0.009, 0.071) * 8;
	var rev;
	var hits = TDuty.kr(Dseq([11, 1]/(145/60), inf));
	var snd = TGrains.ar(2, trig, ~buf2, rate, phase, 0.018, 0, 4, 2).sum;
	snd = HPF.ar(LPF.ar(snd, 10000), 100);
	snd = MidEQ.ar(snd, 510, 0.3, -24);
	snd = snd * (Decay.kr(hits, 0.5)*12).dbamp;
	snd = (snd * -6.dbamp).softclip;
	rev = GVerb.ar(snd.sum, 88, 5, 0.3, 0.7, 24, -6.dbamp, 0.4, -12.dbamp, 300, -12.dbamp, 0);
/*	snd = (snd * -6.dbamp).softclip;*/
	snd = Compander.ar(rev, snd, -6.dbamp, 1, 1/4, 0.08, 4.3);
	snd = snd * (1-LPF.ar(LFPulse.ar({LFNoise1.kr(1/3).range(8, 13)}!4, 0, 0.7, Decay2.kr({Trig1.kr(Dust.kr(1/8))}!4, 2.3, 4.8).clip(0, 1)), 40););
/*	snd = MidEQ.ar(snd, 15200, 1.3, 24);*/
/*	(snd[0] - snd[1]).poll;*/
	Limiter.ar(snd, -3.dbamp);
/*	LPF.ar(LFPulse.ar(13, 0, 0.5, Decay2.kr({Dust.kr(1/4)}!2, 1.3, 8.8)), 40);*/
	
})
Ndef(\shine1).source = nil
Ndef(\shine1).free



///////////////////////// dam dam


~dam = Buffer.read(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/damdam.aif")
~dam1 = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/damdam.aif", 0, -1, 0);
Ndef.audio(\dam, 4)
Ndef(\dam).source = {
	var trig = Impulse.kr(1/(12/(145/60)));
	var snd = PlayBuf.ar(2, ~dam, 0.9, trig, 0, 0);
/*	var del = CombC.ar(snd, 145/60*2, 145/60/2, 145/60*8, 1) +snd;*/
	var graintrig = Dust.ar(288);
	var grainsnd = TGrains.ar(2, graintrig, ~dam1, 0.9, 0.7 + TRand.ar(-0.05, 0.05, graintrig));
	grainsnd = grainsnd * EnvGen.kr(Env.perc(1.3, 5, 8.dbamp), TDelay.kr(trig, 1));
	snd = snd + grainsnd;
	
/*	snd = PitchShift.ar(snd, 0.2, 1/(TIRand.kr(2, 8, Impulse.kr(4)))) + snd;*/
	snd = PitchShift.ar(snd, 0.2, 1/Line.kr(8, 2, 22).round() / 0.9, 0, 0.1, 8);
	snd = snd * EnvGen.kr(Env.perc(0.03, 0.1, 6.dbamp), TDelay.kr(trig, 0.1) + TDelay.kr(trig, (145/60)/4)) + snd;
	snd = Limiter.ar(snd.softclip, -3.dbamp);
	Out.ar(~out, snd.sum!4);
}
Ndef(\dam).clear
Ndef(\dam).pause
Ndef(\dam).resume
~out.scope




