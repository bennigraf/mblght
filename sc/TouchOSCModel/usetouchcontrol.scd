TouchControl.replAddr = NetAddr("192.168.1.22", 5001);
TouchControl.replAddr = NetAddr("169.254.209.137", 5001);
x = TouchControl.new(\fader, '/main/param1');

x.bus.getSynchronous
x.val
x.set(1.0.rand)
x.bus.setSynchronous(1.0.rand)

x.dump

y.free;
y = {
/*	Out.kr(x.bus, LagUD.kr(Amplitude.kr(SoundIn.ar(0)), 0.01, 3));*/
	Out.kr(x.bus, Decay.kr(Dust.kr(12/10), 0.5));
}.play

