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


n.add(\neg, NodeProxy.control(s, 60))
(n[\neg][0] = {
	var outs = 0!60;
	var src = Hsv2rgb.kr(0.7, 1, 1);
	var env = EnvGen.kr(Env.triangle(\myDur.kr(1)), \t_trig.tr).lincurve(0, 1, 0, 1, -2);
	var dmpr = PanAz.kr(20, DC.kr(1), \pos.kr(0), \lvl.kr(0), \width.kr(3).lag(0.1), -0.5) * env;
	{ |i|
		{ |j|
			outs[i*3+j] = src[j] * (1-dmpr[i]);
		}!3;
	}!20;
	
/*	outs = outs * (1 - dmpr);*/
	
	outs;
});
(
var pb = Pbind(
	\dur, Pseq([3, 2, 3, 4], inf),
/*	\pos, Pseq((1,2..16)/15, 1)/0.9,*/
	\pos, Pseq([1.5, 1.1, 2, 0.6, 0.1].reverse, 1),
/*	\pos, Pwhite(0.0, 2.0),*/
/*	\pos, 0.5,*/
/*	\lvl, Pseq((0.1,0.2..1), 4),*/
	\lvl, 1,
	\width, Pseq((2,4..10)*2, 1),
	\t_trig, 1,
	\myDur, Pfunc({|ev|ev.dur}),
);
t.record;
n[\neg][1] = \set -> pb;
Ndef(\full->\internal)[1] = \set -> pb;
);
t.stopRecording


Ndef.ar(\full->\internal, 4);

(Ndef(\full->\internal, {
	var snd, out;
	var env = EnvGen.kr(Env.triangle(\myDur.kr(1)), \t_trig.tr).lincurve(0, 1, 0, 1, -2);
	var dmpr = PanAz.kr(20, DC.kr(1), \pos.kr(0), \lvl.kr(0), \width.kr(3).lag(1), -0.5) * env;
	snd = { |n|
		var freq = (80 - (n * 2));
		RLPF.ar(Impulse.ar(freq.midicps), (freq + 8 + LFNoise2.kr(3.7, 4)).midicps, 0.1) * -12.dbamp;
	}!20;
	snd = snd * dmpr;
	out = 0!4;
	20.do{ |n|
		out = out + PanAz.ar(4, snd[n], 2/19 * n, 1, 0.5);
	};
	Compander.ar(out, out, -12.dbamp, 1, 1/4, 0.01, 0.1);
}));
Ndef(\full->\internal).play






