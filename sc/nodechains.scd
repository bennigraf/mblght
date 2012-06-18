o.close
d.removeDevice(0)
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
Patcher.all[\stage].devices[0].buses[\color].setn([0, 1, 1])
p.devices
s.waitForBoot({p.makeBusForMethod(\color, 3, nil, 20);})

p.busesForMethod(\color)
p.busesForGroupMethod(\ring, \color)

p.message((method: \init))
p.devices.do({ |dev, i|
	dev.device.action(\init);
})
p.message((method: \color, data: [0, 0.5, 0.1]))

n = ProxyChain()
n.add(NodeProxy.control(s, 60).source = { 
	Out.kr(p.busesForMethod(\color), \in.kr(0!60));
})
n.stickToBottom()
n.nodes.size
n.nodes[2].source = {
	var in = \in.kr(0!60);
	p.busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
}
n.add(NodeProxy().source = {
	SinOsc.kr()!3;
});
n.nodes[0].bus
n.nodes[0].source = {
	var hsv = [MouseX.kr(0, 1), 1, 1];
	// stopped here: need to implement hsv on the server... fuck!
	
	SinOsc.kr(0.1, 0, 0.3, 0.65)!3;
	
}
n.nodes[0].source = { [1, 1, 1] }

n.add(NodeProxy.control(s, 3))
n.nodes[1].source = {
	var in = \in.kr(0!3);
	DelayN.kr(in, 0.5, [0, 0.25, 0.5]);
}

n.add(NodeProxy.control(s, 60))
(
n.nodes[1].source = {
	var in = \in.kr(0!3);
/*	in = in * (1 - ({Decay.kr(Dust.kr(0.01), 2)!3}!20).flatten );*/
/*	in = (in * (1 - Decay.kr(Dust.kr(1/4), 2))).flatten;*/
/*	in = [1, 1, 1];*/
	var width = 4;
	var ori = -0.5;
	var inhlpr = [
		PanAz.kr(20, in[0], MouseY.kr(0, 2), 1, width, ori),
		PanAz.kr(20, in[1], MouseY.kr(0, 2), 1, width, ori),
		PanAz.kr(20, in[2], MouseY.kr(0, 2), 1, width, ori)
	];
	var out = 0!60;
/*	inhlpr[0].dump;*/
	out.do({ |outobj, i|
/*		("out "+i+" is inhlpr["+(i%3)+"]["+(i/3).floor+"]").postln;*/
		out[i] = inhlpr[i%3][(i/3).floor];
	});
/*	in = PanAz.kr(20, in, MouseY.kr(0, 2), 1, 2, 0).flatten;*/
/*	out.poll;*/
/*	in.poll;*/
}
)

n.remove(1)





x = NodeProxy(s, \audio, numChannels: 14).source = {
	Out.ar(0, \in.ar(0!2))
}
x.source = {Out.ar(0, \in.ar(0!14))}
Bus
y = NodeProxy.audio(s, 14).source = { SinOsc.ar([400, 3049 + SinOsc.kr(0.5, 0, 200)]) }
y.source = { [Saw.ar(400), SinOsc.ar(3049 + SinOsc.kr(0.5, 0, 200), 0, 8), Impulse.ar(200)].softclip/10 }
x.map(\in, y)

x.bus.getn

x.unmap(\in)
x.free
y.free




