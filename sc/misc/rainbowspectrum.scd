// spectrum analyzer on rainbowduino board
s.quit;
s.options.device_("JackRouter");
s.boot;

p = Patcher.new(\spectrum);

b = DmxBuffer.new();
b.fps = 30;
SerialPort.devices;
r = RainbowSerial.new(3);

b.addDevice(r);

r.close
b.removeDevice(0)
b.devices

p.addBuffer(b);

64.do({ |i|
	p.addDevice(Device.new(\smplrgb, i * 3));
});

// p.devices
// p.busesForMethod(\color)[0].setn([1, 0, 1])

(
x.free;
x = {
	var freqs = (38,46..98).midicps;
	var buses = p.busesForMethod(\color);
	var in = SoundIn.ar([0, 1]).sum/2;
	var ampcorr = A2K.kr(Amplitude.ar(in, 0.01, 0.1).ampdb.abs.lagud(180, 0.001)) - 3;
	var amps = A2K.kr(Amplitude.ar(BPF.ar(in, freqs), 0.01, 0.1)).ampdb;
	var huemod = LFNoise2.kr(1/18) + 1;
	var satmod = LFNoise2.kr(1/9) + 1 / 4 * 0;
/*	var lagamps = amps.abs.lagud(80, 0.001);*/
	var modamps = amps + ampcorr;
	modamps.do{ |amp, i|
		8.do{ |j|
			var aval = (amp + (28 - (j*3))).clip(0, 1).lagud(0.1, 1);
			// could remove wrapping, now in Hsv2rgb-Ugen
			var clr = Hsv2rgb.kr((huemod + (2/6 * (j/8))).wrap(0, 1), 1-satmod, aval);
			Out.kr(buses[i*8 + j], clr);
		}
	}
}.play;)