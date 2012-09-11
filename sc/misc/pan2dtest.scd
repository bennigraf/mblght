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
var gridx = 8, gridy = 8;
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
/*	var posx = SinOsc.kr(1/3) / 2,
		posy = SinOsc.kr(1/3, pi/2) / 2;*/
	
	// assuming grid outputs stuff l2r t2b 
	// posx/posy (-1 to 1) is actually the phase of a sine (0 to pi) that's spread
	// over the whole grid
	
	var buses = p.busesForMethod(\color);
	
	var outs = 0!(gridx * gridy);
	
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
	p.busesForMethod(\color).do({ |bus, i|
		Out.kr(bus, (color - (0.02 * i)).fold(0, 1));
	});
}).play;