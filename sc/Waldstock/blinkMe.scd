LGui()


n = ProxyChain()
n.clear(true)
n.add(\player, NodeProxy(s));
n.stickToBottom();
(n[\player].source = {
	var in = \in.kr(0!48);
	Patcher.all.at(\waldstock).busesForGroupMethod(\ring, \color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
});
n.printChain



n.add(\clr, NodeProxy.control(s, 48))

(n[\clr].source = {
	var sig = ({|n| Hsv2rgb.kr(SinOsc.kr(1/24, 2/16*n)+1/2, 1, 1); }!16).flatten;
/*	var sig = Hsv2rgb.kr(LFSaw.kr(1/13, 0, 0.5, 0.5), 1, 1);*/
/*	var sig = ({2.rand}!48).flatten;*/
	var pos = LFSaw.kr(1/14, 0, 1, 1);
	sig = sig * Blitzen.kr(16, 3, rate: 8, pos: pos, width: 2, ori: -0.5).clip;
/*	sig = sig + DelayL.kr(Decay2.kr(Mirror.kr(16,3,sig,1), 0.5, 1.5, 0.3), 0.15, 0.15);*/
	sig = sig + (Decay.kr(Mirror.kr(16,3,sig,1), 2).clip * 0.01);
	sig;
})
n[\clr].source = nil
n.remove(\clr)


Patcher.all.at(\waldstock).busesForGroupMethod(\ring, \color).do({ |bus|
	bus.setn([0, 1, 0])
})
Patcher.all.at(\waldstock).busesForGroupMethod(\stage, \color).do({ |bus|
	bus.setn([0, 0, 1])
})
Patcher.all.at(\waldstock).busesForMethod(\color).do({|bus|
	bus.setn([1, 0, 0]);
})



m = ProxyChain()
m.add(\player, NodeProxy(s))
m.stickToBottom();
m[\player].source = {
	var in = \in.kr(0!18);
	Patcher.all.at(\waldstock).busesForGroupMethod(\stage, \color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [in[offset], in[offset+1], in[offset+2]]);
	});
}
m.add(\src, NodeProxy.control(s, 18))
m.printChain
m[\src].source = {
/*	([0, 0.5, 1]!6).flatten * DC.kr(1);*/
	var trigs = Impulse.kr(1/2) +
				Impulse.kr(1/4, 2.5/8) +
				Impulse.kr(1/4, 1/8);
	var trigs2 = Impulse.kr(1/4, 3/4);
	var clr = Hsv2rgb.kr(0, 1, 1) * Decay.kr(trigs, 0.5);
	var out = {|n|
		clr * DelayL.kr(clr, 0.3, 0.3/3*n);
	}!3;
/*	var rlout = Mirror.kr(6, 3, (out ++ out).flatten, 1);*/
	out = (out ++ out).flatten;
	out = out + (([0, 0, 1]!6).flatten * Decay.kr(trigs2, 1));
	out;
}
m.remove(\src)
m.clear(true)