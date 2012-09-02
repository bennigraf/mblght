// node1: delayline: duration, decay
// node2: mirror: lag/decay

Routine.run({
	// white
	Ndef(\backnode1).kr();
	Ndef(\backnode1).source_({
		var buses = Patcher.default.busesForGroupMethod(\ring, \color);
		var stagebuses = Patcher.default.busesForGroupMethod(\stage, \color);
		var left = In.kr(stagebuses[9], 3) + In.kr(stagebuses[8], 3) + In.kr(stagebuses[10], 3) + In.kr(stagebuses[11], 3) / 2; 
		var right = In.kr(stagebuses[2], 3) + In.kr(stagebuses[3], 3) + In.kr(stagebuses[0], 3) + In.kr(stagebuses[1], 3) / 2;
/*	var	left = Hsv2rgb.kr(LFNoise2.kr(1/18.42), 1, 1) * LFNoise0.kr(8).range(0, 0.4) + 0.1 * (1-Decay2.kr(Dust.kr(1/12), 3, 8));*/
/*	var	right = Hsv2rgb.kr(LFNoise2.kr(1/18.42), 1, 1) * LFNoise0.kr(8).range(0, 0.4) + 0.1 * (1-Decay2.kr(Dust.kr(1/12), 3, 8));*/
		var sig = 0!(buses.size*3);
		var sigleft = {|n|
			DelayL.kr(left, 2, 1.3/(buses.size/2) * (n+1));
		}!(buses.size/2);
		var sigright = {|n|
			DelayL.kr(left, 2, 1.3/(buses.size/2) * (((buses.size)/2).floor - n));
		}!(buses.size/2);
/*		buses.size.postln;*/
/*		right.poll(2);*/
		sig = [sigleft ++ sigright];
/*		sig[2].poll(2);*/
/*		sigleft.size.postln;*/
/*		sig[0][0][].size.postln;*/
/*		sig = left;*/
/*		sig.poll(2);*/
		sig = sig.lag2(0.062);
/*		sig = Decay.kr(sig, \decay.kr(0.5) * 4);*/
		buses.do({ |bus, i|
			Out.kr(bus, sig[0][i] * \sw.kr(1));
		});
		0;
	});
	s.sync;
	// col
	Ndef(\backnode2).kr();
(	Ndef(\backnode2).source_({
		var buses = Patcher.default.busesForGroupMethod(\ring, \color);
		var stagebuses = Patcher.default.busesForGroupMethod(\stage, \color);
		
		var sig = { |n|
			stagebuses[n].postln;
			MultiPanAz.kr(buses.size, In.kr(stagebuses[n], 3), 2/stagebuses.size * n, 2);
		}!stagebuses.size;
		
		sig = (LagUD.kr(sig.sum, 0, 0.01) * 2).clip(0, 1);
		sig.size.postln;
		buses.do({ |bus, i|
			"out".postln;
			Out.kr(bus, [sig[i*3+0], sig[i*3+1], sig[i*3+2]] * \amp.kr(0) * \sw.kr(0));
		});
		0;
	}));
	Ndef(\backnode2).set(\sw, 1)
	Ndef(\backnode2).set(\amp, 1)

	// white
	MIDIdef.noteOn(\backnode1, { |msg|
		if(Ndef(\backnode1).get(\sw) == 1, {
			Ndef(\backnode1).set(\sw, 0);
			~mout.noteOff(0, 9);
		}, {
			Ndef(\backnode1).set(\sw, 1);
			~mout.noteOn(0, 9);
		});
	}, 9);
	MIDIdef.cc(\node1a, { |val|
		Ndef(\backnode1).set(\amp, val/127);
	}, 9);
	
})
