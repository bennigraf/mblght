n = ProxyChain();

n.clear(true)

n.add(\player, NodeProxy(s));
n.stickToBottom();
(n[\player].source = {
	var in = \in.kr(0!60);
/*	[in[0], in[1], in[2]].poll;*/
	Patcher.all.at(\waldstock).busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
});
n.printChain


n.add(\rev, NodeProxy.control(s, 60))
n.remove(\rev)
(n[\rev][0] = { 
	var trig = Dust.kr(1);
	var src = ({ TRand.kr(0.0, 1.0, trig) }!3 * 2 * Decay.kr(trig, 4)).clip;
	
	var ins = MultiPanAz.kr(20, src, TRand.kr(0.0, 2.0, trig));
	var outs = 0!60;
	20.do({ |c|
/*		var revSignl = max(max(ins[c], ins[c+1]), ins[c+2]) * SinOsc.ar(1000);*/
		var revSignl = max(max(src[0], src[1]), src[2]) * SinOsc.ar(1000);
		var rev = DelayL.kr(Amplitude.kr(FreeVerb.ar(revSignl, 1, 1, 0), 0.1, 0.3), 0.1, 0.1);
		3.do({ |i|
/*			outs[c*3+i] = ins[c*3+i] * rev;*/
			outs[c*3+i] = src[i] * rev;
		});
	});
	outs = outs + ins;
})

(
SynthDef(\lightver, { 
	var in = DC.kr(0)!60;
/*	var trig = 1;*/
	var src = Hsv2rgb.kr(\key.kr()*6, Rand(0.4, 1.0), 1) * 2 * EnvGen.kr(Env.perc(0.1, 3));
	
	var ins = MultiPanAz.kr(20, src, \pos.kr() * 2, 2, -0.5);
	var outs = 0!60;
	
	var revSignl = max(max(src[0], src[1]), src[2]) * SinOsc.ar(1000);
	var rev = Amplitude.kr(FreeVerb.ar(revSignl, 1, 0.99), 0.1, 0.3).lag(0.1) * 0.3;
	20.do({ |c|
/*		var revSignl = max(max(ins[c], ins[c+1]), ins[c+2]) * SinOsc.ar(1000);*/
		var mod = LFNoise1.kr(13, 0.1, 0.9);
		3.do({ |i|
/*			outs[c*3+i] = ins[c*3+i] * rev;*/
			outs[c*3+i] = src[i] * rev * mod;
		});
	});
	outs = outs + ins;
	
	DetectSilence.kr(outs.sum, doneAction: 2);
	
	Patcher.all.at(\waldstock).busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [outs[offset], outs[offset+1], outs[offset+2]]);
	});
}).add;
)
Synth(\lightver);

(
SynthDef(\realver, { 
	var snd = SinOsc.ar((\key.kr * 12 + 80).round(2).midicps, 0, 100).tanh.softclip
	 	* EnvGen.kr(Env.perc(0, 0.31));
	var out = PanAz.ar(4, snd, \pos.kr*2, 1, 2, 0.5);
	var rev = FreeVerb.ar(snd, 1, 0.9, 0.2, 0.6);
	out = LPF.ar(out + rev, 12000);
	DetectSilence.ar(out.sum, doneAction: 2);
	Out.ar(0, out);
}).add;
)
Synth(\realver, [\pos, 1.0.rand, \key, 1.0.rand], Server.internal);

(
var dur = Pseq([Pseq([2], 3), 4, Pseq([1], 4), 4, 2, 3, 2]);
var key = Pwhite(0.0, 1.0);
var pos = Pwhite(0.0, 1.0);
t.record;
/*var pos = Pseq([0, 0.5, 1, 1.5, 2]/2, inf);*/
p.stop; q.stop;
p = Pbind(
	\instrument, \lightver,
	\dur, dur,
	\key, key,
	\pos, pos,
	\some, Pfunc({|e|
		(server: Server.internal, instrument: \realver,
			dur: e.dur, key: e.key, pos: e.pos).play;
	})
).play;
)
t.stopRecording


