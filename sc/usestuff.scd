OlaPipe

d = DmxBuffer.new();
d.fps = 100

o = OlaPipe();
o.send(List.newUsing([13, 55, 231, 133]))

d.addDevice(o);

d.set(List.newUsing([13,14,15,16,17,198]), 0)

t = Task({
	inf.do({
		d.set(512.rand, 255.rand);
		0.01.wait;
	});
}).play
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
r.send(List.newUsing([0, 0, 200, 255]))
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
r = RainbowSerial(3);
d.addDevice(r);

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
7.do({ |i|
	p.addDevice(Device(\smplrgbpar, i * 3));
	p.addDevice(Device(\smplrgbpar, 7*3 + (8 * 3 * i)));
	p.addDevice(Device(\smplrgbpar, 189 - (i * 3)));
	p.addDevice(Device(\smplrgbpar, 189 - (7 * 3) - (i * (8*3))));
});
)

(
t = Task({
	var clr = {255.rand}!3;
	28.do({ |j|
		n.sendMsg('/stage/devices', j, 'color', clr[0], clr[1], clr[2]);
		0.03.wait;
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

