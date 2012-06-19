(
Routine.run({
	d = DmxBuffer.new();
	d.fps = 30;
	o = OlaPipe(0);
	d.addDevice(o);
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
p.message((method: \color, data: [0, 1, 1]))



n = ProxyChain()

n.add(\player, NodeProxy(s))
n.stickToBottom()
(n[\player].source = {
	var in = \in.kr(0!60);
	p.busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
})
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
		Hsv2rgb.kr(SinOsc.kr(1/38, pi / 20 * i, 0.1, 0.7), 1, 0.3);
	}!20;
/*	rgb.poll;*/
	rgb.flatten;
})


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

n.add(\rotor, NodeProxy.control(s, 60))
n.moveUp(\rotor)
(n[\rotor].source = {
	var in = \in.kr(0!60);
	var channels = { |i| [in[i * 3], in[i*3+1], in[i*3+1]]; }!20;
	var pos = LFSaw.kr(1/20);
	var hlpr = { |i| 
		PanAz.kr(20, channels[i], pos);
	}!20;
	var out = { |i|
		// i don't fucking get it, go on later....'
		out[i] = hlpr[0][r][i];
		out[i+1] = hlpr[0][i+1][i]
		out[i+2] = 
	}!20;
	{ |i|
		hlpr[i] = hlpr[i].flop;
	}!20;
	out = out;
	out => 20
	out[0] => 3
	out[0][0] => 20
	out[0][0][0].size.postln;
	out;
})
n.moveDown(\rotor)

111 222 333 444 ...
[[1, 1, 1], [2, 2, 2], [3, 3, 3]]

a = [[1, 0, 0, 0, 0], [1, 0, 0, 0, 0], [1, 0, 0, 0, 0]].flop
a.flop


[[[1, 1, 1], [2, 2, 2], [3, 3, 3]], [[1, 1, 1], [2, 2, 2], [3, 3, 3]]]



Ndef(\tester, {
	var snd = DC.kr(1) * [1, 2, 3];
	snd = PanAz.kr(20, snd);
	snd[2].poll;
});
Ndef(\tester).free