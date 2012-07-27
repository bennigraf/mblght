
// grumbeling to begin with

~buf2 = 0!2
~buf2[0] = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/shine-intro.aif", 0, -1, 0)
~buf2[1] = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/shine-intro.aif", 0, -1, 1)

Ndef.audio(\shine1, 4)
Ndef(\shine1).play
/*Ndef(\shine1).playN([0, 1, 6, 7]);*/
(Ndef(\shine1).source = {
	// \amp, \hitintens
	var trig = Impulse.kr(LFNoise1.kr(3).range(8, 40) * 10) + Dust.kr(18);
	var phase = 2.27327 * TRand.ar(1.0, 1.15, trig);
	var rate = 0.13;
	var windowsize = TRand.ar(0.009, 0.071) * 8;
	var rev;
	var hits = TDuty.kr(Dseq([1,  11]/(145/60), inf), \reset.tr(1));
	var snd = TGrains.ar(2, trig, ~buf2, rate, phase, 0.018, 0, 4, 2).sum;
	snd = HPF.ar(LPF.ar(snd, 10000), 100);
	snd = MidEQ.ar(snd, 510, 0.3, -24);
	snd = snd * (Decay.kr(hits, 0.5, \hitintens.kr(0))*12).dbamp;
	snd = (snd * -6.dbamp).softclip;
	rev = { GVerb.ar(snd.sum, 88, 5, 0.3, 0.7, 24, -6.dbamp, 0.4, -12.dbamp, 300, -12.dbamp, 0).sum }!4;
	snd = Compander.ar(rev, snd, -6.dbamp, 1, 1/4, 0.08, 4.3);
	snd = snd * (1-LPF.ar(LFPulse.ar({LFNoise1.kr(1/3).range(8, 13)}!4, 0, 0.7, Decay2.kr({Trig1.kr(Dust.kr(1/8))}!4, 2.3, 4.8).clip(0, 1)), 40););
	snd = snd * \amp.kr(1);
	Limiter.ar(snd, -3.dbamp);	
})
Ndef(\shine1).pause
Ndef(\shine1).resume
Ndef(\shine1).stop
Ndef(\shine1).set(\reset, 1)


/////////// dam dam

~dam = Buffer.read(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/damdam.aif")
~dam1 = Buffer.readChannel(s, "/Users/bennigraf/Documents/Studium/2012-SS/bchlr/mblght/sc/Waldstock/Shine/shine-samples/damdam.aif", 0, -1, 0);
Ndef.audio(\dam, 4)
(Ndef(\dam).source = {
	// amp, pitchshift
	var abar = 12/(145/60); // time of 1 bar...
/*	var trig = Impulse.kr(1/(abar));*/
	var trig = TDuty.kr(Dseq([abar], inf), \reset.tr(1));
	var pos = TRand.kr(0, 2, trig);
	var endpos = TChoose.kr(trig, [0.5, -0.5]);
	var snd = PlayBuf.ar(2, ~dam, 0.9, trig, 0, 0);
	var graintrig = Dust.ar(288);
	var grainsnd = TGrains.ar(2, graintrig, ~dam1, 0.9, 0.7 + TRand.ar(-0.05, 0.05, graintrig));
	grainsnd = grainsnd * EnvGen.kr(Env.perc(1.3, 5, 8.dbamp), TDelay.kr(trig, 1));
	snd = snd + grainsnd;
	snd = PitchShift.ar(snd, 0.2, 1/\pitch.kr(2).floor.lag(0.6) / 0.9 + LFNoise1.kr(1/abar, mul: 0.01), 0, 0.1, 8);
	snd = snd * EnvGen.kr(Env.perc(0.03, 0.1, 6.dbamp), TDelay.kr(trig, 0.1) + TDelay.kr(trig, (145/60)/4)) + snd;
	snd = Limiter.ar(snd.softclip, -3.dbamp);
	snd = PanAz.ar(4, snd.sum, (Phasor.kr(trig, 1/ControlRate.ir/abar, 0, 1) * endpos + pos));
	snd = snd * \amp.kr(1);
	Limiter.ar(snd, -3.dbamp);	
})
/*Ndef(\dam).playN([ 0, 1, 6, 7]);*/
Ndef(\dam).play
Ndef(\dam).clear
Ndef(\dam).pause
Ndef(\dam).resume
~out.scope

MIDIClient.init;
MIDIIn.connectAll

// shine1 amp:
MIDIdef.cc(\shine1amp, { |val| 
	Ndef(\shine1).set(\amp, val/127);
}, 1);
MIDIdef.cc(\shine1intens, { |val| 
	Ndef(\shine1).set(\hitintens, val/127);
}, 2);
MIDIdef.cc(\damamp, { |val| 
	Ndef(\dam).set(\amp, val/127);
}, 3);
MIDIdef.cc(\dampitch, { |val| 
	Ndef(\dam).set(\pitch, val);
}, 4);
MIDIdef.noteOn(\reset, {
	"reset".postln;
	Ndef(\dam).set(\reset, 1);
	Ndef(\shine1).set(\reset, 1)
}, nil);




////////////////////////
Ndef.audio(\audio, 4);
Ndef(\audio).play
(Ndef(\audio).source = {
	var panned;
	var snd = SoundIn.ar([0, 1]) * -6.dbamp;
/*	var snd = WhiteNoise.ar(0.15) * Decay.ar(Impulse.ar(1), 0.1)!2;*/
	var del1 = CombC.ar(LPF.ar(snd.sum, 1000, 1.5), 0.14, 0.14, 0.9);
	var del2 = CombC.ar(LPF.ar(del1, 1000, 1.5), 0.14, 0.14, 0.9);
	var rev = FreeVerb2.ar(del1, del2, 0.3 * \wet.kr(0) + 0.2, 0.7, 0.1);
	snd = snd ++ (rev * \wet.kr(0));
	snd = snd * -12.dbamp;
	panned = {|n| PanAz.ar(4, snd[n], 0.5 * n + \rotation.kr(0), orientation: 0) }!4;
/*	panned.flop.sum.poll;*/
	panned.sum;
})

Ndef(\audio).set(\wet, -12.dbamp)

MIDIdef.cc(\roundwet, { |val| 
	Ndef(\audio).set(\wet, val/127);
}, 5);
MIDIdef.cc(\roundrot, { |val| 
	Ndef(\audio).set(\rotation, val/127*2);
}, 6);