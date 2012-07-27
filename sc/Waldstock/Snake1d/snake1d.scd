

// Autosnake:
// a snake wanders around a ring (head green, tail türkis?), must reach next item as fast as possible
// items are being placed here and there, snake must reach them as fast as possible (left or right...)
// snake overlaps items?
// snake cannot hit itself?

// timer: for each aim there's a minimum distance to be travelled. After the distance, there's a countdown for each
// step taken. After 12 such steps, the game is over!

Snake1d.channels = 20
Snake1d.patcher = \waldstock
~game = Snake1d();

~game.speed = 4
~game.stop
~game.resetState

Synth(\snakedesert);

(
SynthDef(\snakehit, { |pos = 0|
	var channels = 20;
	var patcher = \waldstock;
	var time = 0.3;
/*	var clr = DC.kr([1, 0, 0]) * EnvGen.kr(Env([0, 1, 1, 0], [0, 0.28, 0]));*/
	var clr = Hsv2rgb.kr(Line.kr(0, 0.1, time), 1, 1) * EnvGen.kr(Env.perc(time/2, time/2));
	var timings = time - (((0..(channels-1))/channels)*time*2-time).abs;

	var sig = { |n|
		clr * DelayN.kr(clr, time, timings[n]);
/*		clr;*/
	}!channels;
	
	sig = sig.flatten;
	
	sig = Rotator.kr(20, 3, sig, pos, width: 3, lag: 0.1);
	
	FreeSelf.kr(TDelay.kr(DC.kr(1), time));
	
	Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
		Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
	});
}).add;
)
Synth(\snakehit, [\pos, 2.0.rand]);
