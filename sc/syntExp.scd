(
SynthDef(\pan, { |out=0|
	var clr = Silent.ar + 255;
	var pos = MouseY.kr(0, 2);
	var sig = PanAz.kr(28, A2K.kr(clr), pos, 1, 2, 0);
	SendTrig.kr(Impulse.kr(20), (0..27), sig);
}).add;
)

~syn = Synth(\pan);
~syn.nodeID
~syn.free


o = OSCFunc({ |msg| msg.postln }, '/tr', s.addr)

(
SynthDef(\foobar, { |out=0|
	var snd = SinOsc.ar(330);
	snd.poll;
	Out.ar(0, snd);
}).add;
)
~s2 = Synth(\foobar);
~s2.free