LGui()
Patcher.all['2d'].makeDefault

// spectrum analyzer on rainbowduino board
s.quit;
s.options.device_("JackRouter");
s.boot;

p = Patcher.new(\spectrum);

b = DmxBuffer.new();
b.fps = 30;


/*SerialPort.devices;
r = RainbowSerial.new(3);
b.addDevice(r);*/

r.close
b.removeDevice(0)
b.devices

p.addBuffer(b);

64.do({ |i|
	p.addDevice(Device.new(\smplrgb, i * 3));
});

// p.devices
// p.busesForMethod(\color)[0].setn([1, 0, 1])
p = Patcher.all['2d'];

(
x.free;
x = {
/*	var freqs = (38,46..98).midicps;*/
	var freqs = (38,49..98).midicps; // for 6 chans...
	var buses = p.busesForMethod(\color);
	var in = SoundIn.ar([0, 1]).sum/2;
	var ampcorr = A2K.kr(Amplitude.ar(in, 0.01, 0.1).ampdb.abs.lagud(180, 0.001)) - 6;
	var amps = A2K.kr(Amplitude.ar(BPF.ar(in, freqs), 0.01, 0.1)).ampdb;
	var huemod = LFNoise2.kr(1/18) + 1;
	var satmod = LFNoise2.kr(1/9) + 1 / 4 * 0;
/*	var lagamps = amps.abs.lagud(80, 0.001);*/
	var modamps = amps + ampcorr;
	modamps.do{ |amp, i|
/*		8.do{ |j|*/
		6.do{ |j| // 6 chans...
			var aval = (amp + (28 - (j*3))).clip(0, 1).lagud(0.1, 1);
			// could remove wrapping, now in Hsv2rgb-Ugen
/*			var clr = Hsv2rgb.kr((huemod + (2/6 * (j/8))).wrap(0, 1), 1-satmod, aval);*/
			var clr = Hsv2rgb.kr((huemod + (2/6 * (j/6))).wrap(0, 1), 1-satmod, aval); // 6 chans
/*			Out.kr(buses[i*8 + j], clr);*/
			Out.kr(buses[i*6 + j], clr); // 6 chans
		}
	};
	0;
}.play;)

p.havefun
p.enoughfun


Patcher.default.addDevice(Device.new(\camera, 399));

(
y.free;
y = {
	var bus = Patcher.default.busesForMethod(\camerapos);
	var phase = Phasor.kr(0, ControlDur.ir * 2pi / 100, 0, 2pi);
	var posx = SinOsc.kr(0, phase, 0.02, 0.502);
	var posz = SinOsc.kr(0, phase + (pi/2), 0.02, 0.502);
	var posy = SinOsc.kr(0, phase * (LFNoise1.kr(1/2).lag(11) * 0.24), 0.0121, 0.486);
	Out.kr(bus[0], [posx, posy, posz]);
	0;
}.play
)

Patcher.default.message((method: \cameraaim, data: [0.502, 0.494, 0.502]))
Patcher.default.message((method: \camerapos, data: [0.502, 0.507, 0.515]))