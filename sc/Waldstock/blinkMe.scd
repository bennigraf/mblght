LGui()


n = ProxyChain().addPlayer(\waldstock, \ring, \color, 16, 3);
n.clear(true)
n.printChain


n.printChain
n.add(\clr, NodeProxy.control(s, 48))

(n[\clr].source = {
	var sig = ({|n| Hsv2rgb.kr((SinOsc.kr(1/SinOsc.kr(1/4).range(2, 56), pi/16*n)+1/2), 1, 1); }!16).flatten;
	var pos = LFSaw.kr(1/SinOsc.kr(1/3.8).range(1, 8), 0, 1, 1);
/*	var width = Decay.kr(Dust.kr(1), 2) * 7 + 2;*/
	var width = EnvGen.kr(Env.perc(0, 2, 1, 0), Dust.kr(1/2)) * 7 + 2;
	var rate = width + 6;
	sig = sig * Blitzen.kr(16, 3, rate: rate, pos: pos, width: width, ori: -0.5).clip;
/*	sig = sig + DelayL.kr(Decay2.kr(Mirror.kr(16,3,sig,1), 0.5, 1.5, 0.3), 0.15, 0.15);*/
/*	sig = sig + (Decay.kr(Mirror.kr(16,3,sig,1), 2).clip * 0.01);*/
	sig;
})
Env.perc(0, 2, 1, 0).plot
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



m = ProxyChain().addPlayer(\waldstock, \stage, \color, 6, 3);
m.add(\src, NodeProxy.control(s, 18))
m.printChain
m[\src].source = {
/*	([0, 0.5, 1]!6).flatten * DC.kr(1);*/
	var trigs = Impulse.kr(1/2)
/*				+ Impulse.kr(1/4, 2.5/8)*/
/*				+ Impulse.kr(1/4, 1/8)*/
					;
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