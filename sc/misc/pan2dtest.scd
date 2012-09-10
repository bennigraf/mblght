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
	var in = Hsv2rgb.kr(LFNoise2.kr(1/5.7) + (Trig.kr(Dust.kr(1), 0.1)), 1, 1);
/*	var posx = MouseX.kr(-1, 1),
		posy = MouseY.kr(-1, 1);*/
	var posx = SinOsc.kr(1/3) / 2,
		posy = SinOsc.kr(1/3, pi/2) / 2;
	
	// assuming grid outputs stuff l2r t2b 
	// posx/posy (-1 to 1) is actually the phase of a sine (0 to pi) that's spread
	// over the whole grid
	
	var buses = p.busesForMethod(\color);
	
	var outs = DC.kr(0)!(gridx * gridy);
	var pih = pi/2;
	
	var width = LFNoise2.kr(1).linexp(-1, 1, 5, 50);
	
	posx = posx.linlin(-1, 1, 0, pi);
	posy = posy.linlin(-1, 1, 0, pi);
	
	gridy.do{ |j|
		gridx.do{ |i|
			outs[(j * gridx) + i] = in
				* SinOsc.kr(0, pih + posx + (pi * i / gridx) )
				* SinOsc.kr(0, pih + posy + (pi * j / gridy) ) ** width;
		}
	};
	
	outs.do{ |out, i|
		Out.kr(buses[i], out);
/*		out.poll(2);*/
	};
	
}.play;)