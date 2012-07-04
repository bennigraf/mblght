
(
Ndef(\blitz).source = {
	// 2 channels of 3 signals
	var blitz = Blitzen.kr(20, 3, rate: MouseY.kr(0.1, 10), pos: 0, width: MouseX.kr(0, 20));
	blitz;
/*	blitz.poll;*/
	Patcher.all[\waldstock].busesForMethod(\color).do({|bus, i|
		var offset = i * 3;
		Out.kr(bus, [blitz[offset], blitz[offset+1], blitz[offset+2]]);
	});
}
)