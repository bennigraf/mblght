NdefMixer.new(s)


Ndef(\node1, {
	HPF.ar(LPF.ar(SinOsc.ar(mul: 4).softclip, 1000), 400) * Pulse.ar(39);
}).play



Ndef(\node2, {|in|
	In.ar(in) * SinOsc.ar(2)
});

Ndef(\node2).source = {|in|
	In.ar(in) * SinOsc.ar(SinOsc.ar(1/8, 0, 5, 5.2))
}

Ndef(\node2).play
Ndef(\node2).set(\in, Ndef(\node1).bus)
Ndef(\node2).clear
"test".postln;
Ndef(\node1).bus.dump
Ndef(\node1).play
Ndef(\node1).clear

Ndef(\node3, {
	A2K.kr(Amplitude.ar(\in.ar))
});


Ndef(\node3).bus.getSynchronous
Ndef(\node3).map(\in, Ndef(\node1))

Ndef(\node3) <<> Ndef(\node1)


(
var window = Window.new("",Rect(318, 356, 200, 100)).front;
var lev = LevelIndicator.new(window,Rect(0, 0, 200, 100))
	.action_{|v| };
Routine{
	1000.do({
		defer{lev.value_(Ndef(\node3).bus.getSynchronous);};
		0.01.wait;
	});
}.play
)


/////////////////////////////////// working!

// order of ndef execution apparentlly arbitrary
Ndef(\node1, {
	HPF.ar(LPF.ar(SinOsc.ar(mul: 4).softclip, 1000), 400) * Pulse.ar(39);
})
Ndef(\node3, {
	A2K.kr(Amplitude.ar(\in.ar))
});

// chains output of node1 to input of node3:
Ndef(\node3) <<> Ndef(\node1)
// same as:
Ndef(\node3).map(\in, Ndef(\node1))

// get bus value...
Ndef(\node3).bus.getSynchronous

// clear connection...
Ndef(\node3).unmap(\in)

Ndef(\node1).free




///////////////////////////////////// super multi bus out?????

Ndef(\nodemulti, {
	var ctl = SinOsc.kr([1/10, 1/13, 1/19]);
	Out.kr(0, ctl);
/*	ctl;*/
});

Ndef(\nodemulti).bus.getnSynchronous
Ndef(\nodemulti).bus.scope
Ndef(\nodemulti).dump


b = Bus.control(s, 9)
b.scope

c = Bus.newFrom(b, 4, 2)

Ndef(\nbus, {
	Out.kr(c, SinOsc.kr([1/10, 1/13]));
})




