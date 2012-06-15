OlaPipe

d = DmxBuffer.new();
d.fps = 100

o = OlaPipe(1);
o.send(List.newUsing([13, 55, 231, 133]))

d.addDevice(o);

d.set(List.newUsing([13,14,155,16,17,198]), 0)

t = Task({
	inf.do({
		d.set(512.rand, 255.rand);
		0.01.wait;
	});
}).play
t.play
t.stop
o.dump


'/test/3/bla'.matchOSCAddressPattern('/test/?/bla')
(
Device.addType(\rgbpar, (
	channels: 5,
	color: { |args|
		// return list with dmx slots/addresses (starting from 0 for this device) and values
		[[0, args[0]], [1, args[1]], [2, args[2]]];
	},
	strobe: { |onoff|
		if(onoff == "on", {
			this.setDmx(this.addr+4, 255);
		}, {
			this.setDmx(this.addr+4, 0);
		})
	}
));
)



p = Patcher(\stage)
p.dump
p.devicesMsg.value(13)
p.id

n = NetAddr("localhost", 57120);
n.sendMsg('/stage')
n.sendMsg('/stage/devices')
n.sendMsg('/stage/devices', 'color', 13)

p = Patcher(\stage);
p.addDevice(Device(\rgbpar, 0));
p.addDevice(Device(\rgbpar, 17));
p.addDevice(Device(\rgbpar, 155));

p.addBuffer(d);


n = NetAddr("localhost", 57120);
n.sendMsg('/stage/devices', 2, 'color', 13, 188, 240);



p = Patcher();
p.addDevice(Device(\rgbpar), 17); // 17 is the starting address of the rgbpar I add here...
p.addGroup('ring'); // creates a 'ring'
p.addToGroup('ring', p.devices[0]); // add first device to group ring
p.message('/ring/0/color 255 0 0');

t = Int8Array.newClear(192).collect({|i| i});
t[13] = 7






















///////////////// use stuff with rainbow
d = DmxBuffer.new();
r = RainbowSerial();
r = RainbowSerial(3)
r.send(List.newUsing([0, 0, 255, 255, 0, 0, 0, 255]))
r.close

d.addDevice(r);
d.devices
d.removeDevice(0);

(
Device.addType(\smplrgbpar, (
	channels: 3,
	color: { |args|
		// return list with dmx slots/addresses (starting from 0 for this device) and values
		[[0, args[0]], [1, args[1]], [2, args[2]]];
	}
));
)
p = Patcher(\stage);
p.addDevice(Device(\smplrgbpar, 0));
p.addBuffer(d);

n = NetAddr("localhost", 57120);
n.sendMsg('/stage/devices', 0, 'color', 255, 0, 0);

p.devices
p.removeDevice(0)


(
64.do({ |i|
	p.addDevice(Device(\smplrgbpar, i * 3));
})
)
(
t = Task({
	var num = 64;
	inf.do({ |i|
		num.do({ |j|
			var r, g, b;
			r = ((i/90 + (pi/num * (j+(num/3 * 2)))).sin.abs * 255);
			g = ((i/90 + (pi/num * (j+(num/3 * 1)))).sin.abs * 255);
			b = ((i/90 + (pi/num * j)).sin.abs * 255);
/*			['/stage/devices', j, 'color', 0, 0, ((i/50 + (j * 8)).sin.abs * 200).asInteger].postln;*/
			n.sendMsg('/stage/devices', j, 'color', r.asInteger, g.asInteger, b.asInteger);
		});
		(1/30).wait;
	})
});
)
t.play
t.stop

s.dumpOSC(true)
s.dumpOSC(false)



////// use actual stuff
d = DmxBuffer.new();
d.fps = 20
r = RainbowSerial(3);
d.addDevice(r);
d.removeDevice(0)

(
Device.addType(\smplrgbpar, (
	channels: 3,
	color: { |args|
		// return list with dmx slots/addresses (starting from 0 for this device) and values
		[[0, args[0]], [1, args[1]], [2, args[2]]];
	}
));
)
p = Patcher(\stage);
p.addBuffer(d);
n = NetAddr("localhost", 57120);

(
7.do({ |i| p.addDevice(Device(\smplrgbpar, i * 3)); });
7.do({ |i| p.addDevice(Device(\smplrgbpar, 7*3 + (8 * 3 * i))); });
7.do({ |i| p.addDevice(Device(\smplrgbpar, 189 - (i * 3))); });
7.do({ |i| p.addDevice(Device(\smplrgbpar, 189 - (7 * 3) - (i * (8*3)))); });
)

(
t = Task({
	var clr = {255.rand}!3;
	28.do({ |j|
		n.sendMsg('/stage/devices', j, 'color', clr[0], clr[1], clr[2]);
		0.1.wait;
	});
});
t.play
)

p.addGroup(\ring)
p.groups[\ring]
p.addDeviceToGroup(p.devices[0], \ring)
p.devices.do({|dev, i|
	p.addDeviceToGroup(dev, \ring);
});


Tdef(\test1, {
	inf.do({
		n.sendMsg('/stage/devices', 'color', 155, 55, 123);
		0.05.wait;
		n.sendMsg('/stage/groups', 'ring', 'color', 255, 0, 25);
		(1/20).wait;
	});
});
Tdef(\test1).play
Tdef(\test1).stop
(
t = Task({
	var num = 28;
	inf.do({ |i|
		num.do({ |j|
			var r, g, b;
			r = ((i/10 + (pi/num * (j+(num/3 * 2)))).sin.abs * 255);
			g = ((i/10 + (pi/num * (j+(num/3 * 1)))).sin.abs * 255);
			b = ((i/10 + (pi/num * j)).sin.abs * 255);
/*			['/stage/devices', j, 'color', 0, 0, ((i/50 + (j * 8)).sin.abs * 200).asInteger].postln;*/
			n.sendMsg('/stage/devices', j, 'color', r.asInteger, g.asInteger, b.asInteger);
		});
		(1/20).wait;
	})
});
)
t.play
t.stop

64.do({ |j|
	n.sendMsg('/stage/devices', j, 'color', 0, 0, 0);
})

/////////////////////////////////// simple messaging

d = DmxBuffer.new();
d.fps = 50

o = OlaPipe();

d.addDevice(o);

p = Patcher(\stage);
p.addBuffer(d);

(
64.do({ |i|
	p.addDevice(Device(\smplrgbpar, i * 3));
})
p.addGroup(\ring)
p.devices.do({|dev, i|
	p.addDeviceToGroup(dev, \ring);
});
)

(
p.message((
/*	group: \ring,*/
/*	device: (1,3..64),*/
	method: \color,
	data: [255, 133, 255]
));
)


(
t = Task({
	var num = 64;
	inf.do({ |i|
		num.do({ |j|
			var r, g, b;
			r = ((i/10 + (pi/num * (j+(num/3 * 2)))).sin.abs * 255);
			g = ((i/10 + (pi/num * (j+(num/3 * 1)))).sin.abs * 255);
			b = ((i/10 + (pi/num * j)).sin.abs * 255);
/*			['/stage/devices', j, 'color', 0, 0, ((i/50 + (j * 8)).sin.abs * 200).asInteger].postln;*/
/*			n.sendMsg('/stage/devices', j, 'color', r.asInteger, g.asInteger, b.asInteger);*/
			p.message((device: j, method: \color, data: [r.asInteger, g.asInteger, b.asInteger]));
		});
		(1/20).wait;
	})
});
)
t.play
t.stop


/////////////////////// early bus testing
d = DmxBuffer.new();
o = OlaPipe(1);
d.addDevice(o);

p = Patcher(\stage);
p.addBuffer(d);



(
64.do({ |i|
	p.addDevice(Device(\smplrgbpar, i * 3));
});
p.addGroup(\ring);
p.devices.do({|dev, i|
	p.addDeviceToGroup(dev, \ring);
});
)

p.busses
s.waitForBoot({p.makeBusForMethod(\color, 3, \ring);})
p.busses[0].bus.getnSynchronous
p.busses[0].bus.setn([0, 0.3, 0.9])

Ndef(\test, {
	var ctl = SinOsc.kr([1/10, 1/13, 1/19], 0, 0.5, 0.5);
	Out.kr(p.busses[0].bus, ctl);
});


///////////////////////////// testing with actual simulator... yay!
d = DmxBuffer.new();
d.fps = 30

o = OlaPipe(0);
d.addDevice(o);

p = Patcher(\stage);
p.addBuffer(d);

(
20.do({ |i|
	p.addDevice(Device(\robeCw1200E, i * 17), \ring);
});
)
(
p.message((method: \color, data: Color.magenta.asArray ));
p.message((method: \color, data: [0, 0, 1] ));
p.message((method: \cmyk, data: [1, 0, 0, 0] ));
)
p.message((method: \color, data: [0, 1, 0] ));
p.message((method: \zoom, data: [0.1]))

(
t = Task({
	var num = 20;
	inf.do({ |i|
		num.do({ |j|
			var r, g, b;
			r = ((i/10 + (pi/num * (j+(num/3 * 2)))).sin.abs);
			g = ((i/10 + (pi/num * (j+(num/3 * 1)))).sin.abs * 255);
			b = ((i/10 + (pi/num * j)).sin.abs * 255);
/*			p.message((device: j, method: \color, data: [r, 0, 0]));*/
			p.message((device: j, method: \zoom, data: [r]));
/*			p.message((device: j, method: \cmyk, data: [g, 0, 1, 1-g]));*/
		});
		(1/30).wait;
	})
});
)
t.play
t.stop


p.message((method: \strobe, data: [0.1]))


s.waitForBoot({p.makeBusForMethod(\strobe, 1);})
s.waitForBoot({p.makeBusForMethod(\zoom, 1);})
p.busses[0].bus.getnSynchronous
p.busses[0].bus.set(0)

Ndef(\test, {
	var ctl = LFSaw.kr(1/5, 1, 0.065, 0.81);
/*	ctl = MouseY.kr(0.81 - 0.065, 0.81 + 0.065);*/
	Out.kr(p.busses[0].bus, ctl);
});
Ndef(\test).pause

Ndef(\test, {
	var ctl = SinOsc.kr(1/5, 0, 0.5, 0.5);
	Out.kr(p.busses[0].bus, ctl);
});


p.busses[0].bus.getnSynchronous
p.busses[0].bus.setn([0, 1, 0])
p.busses[0].routine.stop
p.removeBus(0)
p.message((method: \color, data: p.busses[0].bus.getnSynchronous))
p.message((method: \color, data: [0, 1, 1]))


///////////////////////////// Node-testing
(
d = DmxBuffer.new();
d.fps = 30;

o = OlaPipe(0);
d.addDevice(o);

p = Patcher(\stage);
p.addBuffer(d);

p.addDevice(Device(\robeCw1200E, 0), \ring);

p.devices[0].buses[\color].setn([1, 1, 0])
p.removeDevice(0)
p.devices

20.do({ |i|
	p.addDevice(Device(\robeCw1200E, i * 17), \ring);
});
)

s.waitForBoot({p.makeBusForMethod(\color, 3, nil, 20);})
p.busses[0].bus.setn((0..60).collect({|i| i % 2}))
p.numDevices(\ring)
Ndef(\test).source = { }
(
Ndef(\test, {
	var trig = Dust.kr(Line.kr(0.5, 2, 10));
	var clr = 3.collect({ TRand.kr(0.0, 1.0, trig) });
	var ctl = (0..59).collect({ |i|
		var dng = clr * Decay.kr(trig, 3);
		DelayN.kr(dng.wrapAt(i), 2, 2/20 * (i/3).floor);
	});
	Out.kr(p.busses[0].bus, ctl);
});
)
