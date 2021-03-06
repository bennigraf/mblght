LGui()
Patcher.default.message((method: \color, data:[0, 1, 1], devices: [2]))
Patcher.default.devices.do({|dev|
	dev.device.dump;
})
Patcher.all.at('2d').makeDefault
Patcher.all.at('2d').devices[0].device.dump
Patcher.default.busesForMethod(\color).size
p = Patcher.new(\spectrum);

b = DmxBuffer.new();
SerialPort.devices;
r = RainbowSerial.new(3);

b.addDevice(r);

p.addBuffer(b);

64.do({ |i|
	p.addDevice(Device.new(\smplrgb, i * 3));
});


(
var gridx = 6, gridy = 6;
x.free;
x = {
	arg posx = 0,
		posy = 0,
		width = 0;
/*	var in = Hsv2rgb.kr(LFNoise2.kr(1/5.7) + (Trig.kr(Dust.kr(1), 0.1)), 1, 1);*/
	var in = Hsv2rgb.kr(LFNoise1.kr(1/5.7), 1, 1);
/*	var in = [1, 0, 0];*/
/*	var posx = MouseX.kr(-1, 1),
		posy = MouseY.kr(-1, 1);*/
	
	// assuming grid outputs stuff l2r t2b 
	// posx/posy (-1 to 1) is actually the phase of a sine (0 to pi) that's spread
	// over the whole grid
	
	var buses = Patcher.default.busesForMethod(\color);
	
	var outs = 0!(gridx * gridy);
	
	posx = SinOsc.kr(1/3) / 2;
	posy = SinOsc.kr(1/3.32, pi/2) / 2;
	
	width = SinOsc.kr(1/7)/2+0.5;
	width = width.linexp(0, 1, 50, 1).lag(0.5);
	
	posx = posx.linlin(-1, 1, -pi/2, pi/2).lag(0.5);
	posy = posy.linlin(-1, 1, -pi/2, pi/2).lag(0.5);
	
	gridy.do{ |j|
		gridx.do{ |i|
			outs[(j * gridy) + i] = in
				* (pi/gridx * i + posx ).sin.clip(0, 1)
				* (pi/gridy * j + posy ).sin.clip(0, 1)
				** width;
		}
	};
	outs.do{ |out, i|
		Out.kr(buses[i], out);
	};
	0;
}.play;)

(
y.free;
y = {
	var buses = p.busesForMethod(\color);
	var chans = buses.collect({ |bus| In.kr(bus) });
	chans.poll;
}.play
)


(
MIDIClient.init;
//There are three possibilities to connect for example to the first device:
/*MIDIIn.connect(0, -1101621857);*/
//MIDIIn.connect(0, MIDIClient.sources.at(0).uid);
MIDIIn.connectAll;
)

MIDIdef.cc(\x, { |val|
	x.set(\posx, val / 127 * 2 - 1 * (-1));
}, 24);
MIDIdef.cc(\y, { |val|
	x.set(\posy, val / 127 * 2 - 1);
}, 25);
MIDIdef.cc(\vel, { |val|
	x.set(\width, val / 127);
}, 23);

b.get
p.message((method: \color, data: [0,0,0]))

[1, 2, 3, 4, 5, 6].slide(3, 4).postcs;
a.free
a = SynthDef(\test, {
	var color = SinOsc.kr({0.4.rand}!3 + 0.1, pi.rand, mul: 0.5, add: 0.5);
	Patcher.default.busesForMethod(\color).do({ |bus, i|
		Out.kr(bus, (color - (0.02 * i)).fold(0, 1));
	});
}).play;

(a.free;
a = SynthDef(\test2, { |out=0|
	var in = Hsv2rgb.kr(LFNoise1.kr(12) * 0.1 + LFSaw.kr(1/12).range(0, 1), 1, 1);
	var posx = SinOsc.kr(1/3);
	var posy = SinOsc.kr(1/4);
	var width = 0.5;
	var outs = Pan2d.kr(6, 6, in, posx, posy, width);
	var modulor = SinOsc.kr(1/19).range(0, 1);
	Patcher.default.busesForMethod(\color).do({ |bus, i|
		Out.kr(bus, outs[i]);
	});
}).play;)