TouchControl.replAddr = NetAddr("192.168.1.6", 5001);
TouchControl.replAddr = NetAddr("padrick.local", 5001);
x = TouchControl.new(\fader, '/main/param1');

x.bus.getSynchronous
x.val
x.set(1.0.rand)
x.bus.setSynchronous(1.0.rand)

x.action_({ |val| [val].postln })

x.dump

y.free;
y = {
	Out.kr(x.bus, LagUD.kr(Amplitude.kr(SoundIn.ar(0)), 0.01, 3));
/*	Out.kr(x.bus, Decay.kr(Dust.kr(12/10), 0.5));*/
}.play


z = [];
(
50.do({ |n|
	z = z.add(TouchControl(\fader, '/main/param'++n))
});
)

OSCFunc.trace(true)

NetAddr("localhost", 57120).sendMsg('/test', *['arg1', 'arg2'])

x = TouchControl.new(\multifader, '/lfo1/waveform', false, 20)
x.set({1.0.rand}!20)
v = {1.0.rand}!20;
TouchControl.replAddr.sendMsg("/lfo1/waveform", *v)
TouchControl.replAddr.sendMsg('/lfo1/waveform/2', 0.3)
x.action_({|index, val| [index, val].postln })


Routine.run({
6.do({ |n|
	var tc = TouchControl.new(\multifader, '/lfo'++(n+1)++'/waveform', true, 20);
	s.sync;
	{
		var src = Decay.kr(Dust.kr(1), 1);
		var sig = {|n| DelayL.kr(src, 3/20*n, 3/20*n) }!20;
		Out.kr(tc.bus, sig);
	}.play;
	s.sync;
});
});



x = TouchControl.new(\multitoggle, '/main/color1', false, 6)
x.action_({|val| val.postln})