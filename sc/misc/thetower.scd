(
p = Patcher.new(\thetower);

b = DmxBuffer.new();

o = OlaPipe.new(0);

b.addDevice(o);
// b.removeDevice(0)

// b.devices[0].end
// b.devices.removeAt(0)

p.addBuffer(b);
p.fps = 20;
b.fps = 20;

160.do({ |i|
	p.addDevice(Device.new(\dim, i));
});

Patcher.default.addDevice(Device.new(\camera, 399));
)
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
	var phase = Phasor.kr(0, ControlDur.ir * 2pi / 100, 0, 2pi);
	var posx = SinOsc.kr(0, phase, 0.03, 0.502);
	var posz = SinOsc.kr(0, phase + (pi/2), 0.03, 0.502);
	var posy = SinOsc.kr(0, phase * 0.3852, 0.0061, 0.516);
	Out.kr(bus[0], [posx, posy, posz]);
	0;
}.play
)

Patcher.default.message((method: \cameraaim, data: [0.502, 0.505, 0.502]))
Patcher.default.message((method: \camerapos, data: [0.502, 0.507, 0.515]))



(
z.free;
z = {
	var buses = Patcher.default.busesForMethod(\dim);
	// var channels = { |i| SinOsc.kr(1/12, 2pi / 160 * i, 0.5, 0.5); }!160 ** 1;
	var channels = { |i| SinOsc.kr(1/12, 2pi / 20 * i, 0.5, 0.5); } !20;
	buses.do({|bus, i| Out.kr(bus, channels[(i/8).floor]) });
	// channels.do({|chan, i| Out.kr(buses[i], chan) });
	0;
}.play;
)


(
z.free;
z = {
	var buses = Patcher.default.busesForMethod(\dim);
	// var channels = { |i| SinOsc.kr(1/12, 2pi / 160 * i, 0.5, 0.5); }!160 ** 1;
	var channels = { |i| SinOsc.kr(1/3.8, 2pi / 20 * i, 0.5, 0.5); } !20 ** LFSaw.kr(1/11.4).linexp(-1, 1, 0.5, 100);
	buses.do({|bus, i| Out.kr(bus, Decay2.kr(Dust.kr(channels[(i/8).floor] * 5), 0.5, 1)) });
	// channels.do({|chan, i| Out.kr(buses[i], chan) });
	0;
}.play;
)


/// the tower interactive?
/*
	twitter: various commands
	base: slow and a little decent wobbles, light waves around the bottom 1 or 2 rows...
	
	commands: light up, calm down, blink, faster, slower
	react to direction a tweet comes from?
	
	
	
	
 */
(
z.free;
z = { |h = 4|
	var buses = Patcher.default.busesForMethod(\dim);
	var phasors = Phasor.kr(0, ControlDur.ir * 2 * LFNoise2.kr({0.45.rand}!11 * \intens.kr(1)) * \intens.kr(1), 0, 2pi);
	var ctlr = phasors;
	var outs = 0!160;
	var height = h.lag(2);
	var channels = { |i|  
		var phases = 2pi / 20 * i + ctlr;
		var val = (phases.sin/2+0.5**2).sum/(phases.size);
		8.do({ |j|
			outs[i * 8 + j] = (1 - (val/8 * height).round(1/8)) <= (j / 8);
		});
	}!20;
	buses.do({|bus, i| 
		Out.kr(bus, outs[i].lag(0.7));
	});
	0;
}.play;
)
z.set(\h, 8)
z.set(\intens, 1)






