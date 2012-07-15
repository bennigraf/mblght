n = ProxyChain().addPlayer(\waldstock, \ring, \color, 16, 3);
n.clear(true)

n.add(\position, NodeProxy.control(s, 48))

(n[\position][0] = { 
	var src = [1, 0, 0] * LFNoise1.kr(13, 0.1, 0.9);
	var myDur = \dur.kr(2);
	var myWidth = EnvGen.kr(Env([2, 8, 2], [myDur/3*2, myDur/3]), \trig_t.tr);
	var myPos = EnvGen.kr(Env([0, 1, 2, 0], [myDur/2, myDur/2, 0], [1, -1, 2]), \trig_t.tr);
	var ori = -0.5;
	
	MultiPanAz.kr(16, src, myPos, myWidth, ori);
})
(
/*t.record;*/
n[\position].set(\trig_t, 1, \dur, 13);
Ndef(\pos->\internal).set(\trig_t, 1, \dur, 13);
)
t.stopRecording
t.stopRecording

Ndef(\pos -> \internal).ar(1);
Ndef(\pos->\internal).play
(Ndef(\pos->\internal).source = {
	var snd;
	
	var myDur = \dur.kr(2);
	var myWidth = EnvGen.ar(Env([2, 3, 2], [myDur/3*2, myDur/3]), \trig_t.tr);
	var myPos = EnvGen.ar(Env([0, 2, 0], [myDur, 0]), \trig_t.tr);
	var ori = -0.5;
	
/*	myPos = LFSaw.ar(1/4)/2;*/
	
	snd = { SinOsc.ar(130 + 34.0.rand + LFNoise1.kr(1).range(-10, 10)).tanh }!13;
	snd = LPF.ar(HPF.ar(snd*2, 800).clip, 800);
	snd = snd.sum * -6.dbamp;
	[myPos, myWidth];
	snd = PanAz.ar(4, snd, myPos/2, 1, 2, ori/2);
	snd.sum;
})
Ndef(\pos->\internal).stop
Ndef(\pos->\internal).clear