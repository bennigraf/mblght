
(
s.waitForBoot({
Ndef(\blitz).source = {
	// 2 channels of 3 signals
/*	var blitz = Blitzen.kr(20, 3, rate: MouseY.kr(0.1, 10), pos: 0, width: MouseX.kr(0, 20).poll);*/
/*	var in = Hsv2rgb.kr(LFSaw.kr(1/10)/2+0.5, 1, 1);*/
	var in = Hsv2rgb.kr((LFSaw.kr(1/26)/2+0.5), 1, 1);
/*	var blitz = Blitzen.kr(20, 3, rate: 10, pos: LFSaw.kr(1/10), width: 8, ori: -0.5);*/
	var pos = LFNoise0.kr(1/5.3).poll;
	var blitz = Blitzen.kr(20, 3, rate: 10, pos: pos, width: LFSaw.kr(1/5.3, 1).range(20, 2), ori: -0.5);
/*	blitz = Decay.kr(blitz, 1.8);*/
/*	blitz = (in!20).flatten;*/
	blitz = blitz * in;
	Patcher.all[\waldstock].busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [blitz[offset], blitz[offset+1], blitz[offset+2]]);
	});
};
/*Ndef(\blitz).play;*/
});
)
