p = Patcher.new(\thetower);

b = DmxBuffer.new();

o = OlaPipe.new(0)

b.addDevice(o);
b.removeDevice(0)

b.devices[0].end
b.devices.removeAt(0)

p.addBuffer(b);

160.do({ |i|
	p.addDevice(Device.new(\dim, i));
});

Patcher.default.addDevice(Device.new(\camera, 449));
p.devices
p.removeDevice(0)

p.message((method: \dim, data: [0.01]))
~n = 0;
(Pdef(\test, Pbind(
	\type, \light,
	\dur, 0.1,
	\device, Pseq((0..159), inf),
	\method, \dim,
/*	\data, [1] * Pwhite(0.0, 1.0);*/
	\data, Pfunc({|e| ~n = ~n + 1; [(~n/33).sin / 2 + 0.5] })
));)
Pdef(\test).play
Pdef(\test).stop


(
y.free;
y = {
	var bus = Patcher.default.busesForMethod(\camerapos);
/*	var aim = p.busesForMethod(\cameraaim);*/
	var posx = SinOsc.kr(0.05, 0, 0.015, 0.502);
	var posz = SinOsc.kr(0.05, pi/2, 0.015, 0.502);
	var posy = SinOsc.kr(0.067, 0, 0.0011, 0.498);
	Out.kr(bus[0], [posx, posy, posz]);
	0;
}.play)

Patcher.default.message((method: \cameraaim, data: [0.502, 0.498, 0.502]))
Patcher.default.message((method: \camerapos, data: [0.502, 0.507, 0.515]))