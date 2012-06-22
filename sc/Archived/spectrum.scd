(
Routine.run({
	d = DmxBuffer.new();
	d.fps = 30;
/*	o = OlaPipe(0);*/
/*	d.addDevice(o);*/
	s.waitForBoot({
		p = Patcher(\stage);
		s.sync;
		p.addBuffer(d);
		s.sync;
		20.do({ |i|
			p.addDevice(Device(\robeCw1200E, i * 17), \ring);
		});
	});
	/*p.message((method: \color, data: [0, 1, 0]))*/
/*	p.removeDevice(0)*/
/*	p.devices*/
});
)
p.message((method: \color, data: [1, 0, 1]))

n.clear

n = ProxyChain();

n.add(\player, NodeProxy(s));
n.stickToBottom();
(n[\player].source = {
	var in = \in.kr(0!60);
	p.busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
});
n.printChain

n.add(\panner, NPTempl(\panner, (\in: 3, \out: 20)))
n[\panner].set(\pos, 0.8)
n[\panner].set(\width, 7)

x = NodeProxy.control(s, 1).source = { LFSaw.kr(LFSaw.kr(1/10, 0, 0.5, 0.5).lag(4)) }
n[\panner].map(\pos, x)
n[\panner].unmap(\pos)

n.add(\source, NodeProxy.control(s, 60))
n.moveUp(\source)

(n[\source].source = {
/*	[DC.kr(1), DC.kr(0), DC.kr(0)]*/
	var hsv;
	var rgb = { |i|
		Hsv2rgb.kr(SinOsc.kr(1/4, pi / 20 * i, 0.1, 0.7), 1, 0.6);
	}!20;
/*	rgb.poll;*/
	rgb.flatten;
})

(n[\source].source = {
/*	var rgb = (0!44) ++ (1!2);*/
	var rgb = [1, 0, 0] ++ (0!57);
/*	rgb.size.postln;*/
	(rgb * DC.kr(1));
})

[1, 1, 1] ++ (0!20)


n.add(\spectrum, NodeProxy.control(s, 60))
(n[\spectrum].source = {
	var in = \in.kr(0!60);
	var soundIn = Compander.ar(SoundIn.ar(0), SoundIn.ar(0), 0.3, 1, 1/4);
	var bins = (0..19).collect({ |i|
		var freq = (80/25*i+55).midicps;
		var snd = BPF.ar(soundIn, freq, 0.15);
		freq.postln;
		A2K.kr(Amplitude.ar(snd, releaseTime: 1));
	});
	var out = bins.collect({ |bin| 
		[0, (bin * 5).min(1), 0];
	}).flatten;
	out = out + in;
	out;
})
n.printChain
n.moveUp(\spectrum)

n.add(\rotor, NodeProxy.control(s, 60))
n.moveUp(\rotor)
(n[\rotor].source = {
	var in = \in.kr(0!60);
	var pos = LFSaw.kr(1/20);
/*	var hlpr = { |i| 
		PanAz.kr(20, channels[i], pos);
	}!20;*/
	var out = PanAz.kr(20, in, pos);
	out = out.flop.sum; // may work but there's no offset...
	out;
})
n.moveDown(\rotor)
n.remove(1)
n.printChain
(n[\rotor].source = {
	var in = \in.kr(0!60);
	var channels = { |i| [in[i * 3], in[i*3+1], in[i*3+2]]; }!20;
	var pos = LFSaw.kr(1/20);
	var out = 0!60;
	var panchans = channels.collect({|chan|
/*		chan = [r, g, b]*/
		PanAz.kr(20, chan, pos, 1, 2.2);
		// -> [[r1, r2, r3, ..], [g1, g2, g3, ..], ..]
	});
	// panchans muesste 20 elemente haben, mit je 3 elementen mit je 20 elementen... = 1200!fuck
/*	[in[0], in[1], in[2]].poll;*/
/*	channels[0].poll;*/
	60.do { |i|
		var sum = 0;
		20.do({ |j|
			sum = (sum + panchans[j][i%3].wrapAt((i/3).floor - j));
		});
		out[i] = sum;
	};
/*	[out[0], out[1], out[2]].poll;*/
	out.lag(2);
})

(n[\rotor].source = {
	var in = \in.kr(0!60);
	var pos = LFSaw.kr(1/10);
/*	var width = 2.2;*/	
	// 20 channels in groups of 3
	Rotator.kr(20, 3, in, pos, 7, 15).poll;
})

Ndef(\tester, {
	var snd = DC.kr(1) * [1, 2, 3];
	snd = PanAz.kr(20, snd, MouseY.kr(0, 2));
	snd[2].poll;
});
Ndef(\tester).free