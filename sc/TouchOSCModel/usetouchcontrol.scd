TouchControl.replAddr = NetAddr("192.168.1.22", 5001);
x = TouchControl.new(\fader, '/main/param1');

x.bus.getSynchronous
x.val
x.set(1.0.rand)
x.bus.setSynchronous(1.0.rand)

x.dump

y.free
y = {
	Out.kr(x.bus, LagUD.kr(Amplitude.kr(SoundIn.ar(0)), 0.01, 3));
}.play