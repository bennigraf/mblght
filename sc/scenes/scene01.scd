Patcher.all.at(\waldstock).message((method: \color, data: [0, 1, 1]))


n = ProxyChain();

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

n.clear(true)



n.add(\src, NodeProxy.control(s, 3))
(n[\src].source = {
/*	Hsv2rgb.kr(0.15, 0.58, 1)*/
	Hsv2rgb.kr(0.15, 0.6, 1)
})


n.add(\pan, NodeProxy.control(s, 60))
n[\pan].source = nil
(n[\pan][0] = {
	var in = Hsv2rgb.kr(0.15, 0.58, 1);
	var width = \width.kr(2);
	var ori = \ori.kr(-0.5);
	var myPos = Sweep.kr(\trig.tr, 1/\myDur.kr) * \endPos.kr;
	var myWidth = 1 + (Sweep.kr(\trig.tr, 1/\myDur.kr) * 3);
	var d = 0.15;
	var a = \myFact.kr.linlin(1, 10, \myDur.kr/2, 0.1);
	var r = \myFact.kr.linlin(1, 10, \myDur.kr/2, \myDur.kr) - 0.1 -d;
	in = in * EnvGen.kr(Env.perc(a, r), TDelay.kr(\trig.tr, d)).lincurve(0, 1, 0, 1, 1);
	MultiPanAz.kr(20, in, myPos, myWidth, ori);
});

(
/*t.record;*/
~pb = Pbind(
	\startPos, Pseq([0], inf),
	\endPos, Pwhite(0.5, 1) * Prand([1, -1], inf),
/*	\myFact, Pseq((1,1.5..10), 1),*/
	\myFact, Pseq([Pseq((1,1.4..10), 1), 10 - Pseq((1,1.4..10), 1), Pseq([1], inf)], inf),
/*	\myFact, 1,*/
	\dur, Pwhite(2.5, 5.3, inf) / Pfunc({|ev| ev.myFact.linlin(1, 10, 1, 4) }),
	\myDur, Pfunc({|ev| ev.dur; }),
	\afunc, Pfunc({ |ev|
		ev.myFact.postln;
		Synth(\roar, [\endPos, ev.endPos, \myDur, ev.dur, \myFact, ev.myFact, \trig, 1], t);
	}),
	\trig, 1
);
/*~pb = nil;*/
n[\pan][1] = \set -> ~pb;
o[1] = \set -> ~pb.deepCopy;
)
n[\pan][1] = nil; o[1] = nil;

~apb = Pbind()
~bpb = ~apb.copy
~apb == ~bpb

p = Pbind(\test, Pseq([Pseries(1, 1, 10), 10 - Pseries(1, 1, 10)], inf))
p.next


n.add(\mirror, NodeProxy.control(s, 60));
n[\mirror][0] = { \in.kr(0!60) }
(n[\mirror][0] = {
	var ins = \in.kr(0!60);
	var outs = 0!60;
	{ |i|
		var ofst = (19-i)*3;
		var delmod = LFNoise1.kr(1, 0.1);
		3.do({|j| 
			var orign = ins[i*3+j];
			var mirr = ins[ofst+j];
			mirr = DelayN.kr(mirr, 0.3, 0.2 + delmod, 0.4);
			mirr = (WhiteNoise.kr(0.1, 0.4).lag(0.04) * mirr);
			outs[i*3+j] = orign + mirr;
		});
	}!20;
	outs;
})



n.add(\rotor, NodeProxy.control(s, 60));
n[\rotor][0] = {
	var in = \in.kr(0!60);
	var pos = LFSaw.kr(\freq.kr(0), 1).range(0, 2);
	Rotator.kr(20, 3, in, pos, 2, 0.1);
}
n[\rotor].set(\freq, 1/23.7)
n[\rotor].set(\freq, 0)


n.add(\mixr, NodeProxy.control(s, 60));
n[\mixr].source = {
	[\in.kr(0!60), \in2.kr(0!60), \in3.kr(0!60)].sum;
}
n.moveUp(\mixr)
n.printChain


o = NodeProxy.control(s, 60)
(o[0] = {
	var in = Hsv2rgb.kr(0.25, 0.58, 1);
	var width = \width.kr(2);
	var ori = \ori.kr(-0.5);
	var myPos = Sweep.kr(\trig.tr, 1/\myDur.kr) * \endPos.kr;
	var myWidth = 1 + (Sweep.kr(\trig.tr, 1/\myDur.kr) * 3);
	var calmness = 1/\myFact.kr;
	var d = 0.15;
	var a = \myFact.kr.linlin(1, 10, \myDur.kr/2, 0.1);
	var r = \myFact.kr.linlin(1, 10, \myDur.kr/2, \myDur.kr) - 0.1 -d;
	var out;
	in = in * EnvGen.kr(Env.perc(a, r, 1, 0), TDelay.kr(\trig.tr, d)).lincurve(0, 1, 0, 1, 1);
	out = MultiPanAz.kr(20, in, myPos, myWidth, ori);
	out;
});
o[1] = nil
n[\mixr].map(\in2, o)









m = NodeProxy(s)
m.source = { SinOsc.ar(o.kr.lag(0.1)) * Pulse.ar(20) }
m.play

m[1] = \filter -> { |in| RLPF.ar(in, 1200 * LFSaw.ar(1/8).range(0.1, 1), 0.01) }

m[3] = \set -> Pbind(\freq, Pseq([100, 200, 300, 400], inf), \dur, Pseq([0.5, 1, 0.5]/10, inf))

o = NodeProxy.control(s, 1)
o.source = { |multi = 1| LFSaw.kr(1).range(300, 800) * multi.poll }
o[1] = \set -> Pbind(\multi, Pseq([1, 2, 3, 4], inf), \dur, Pseq([0.5],inf))

m.free
o.free
