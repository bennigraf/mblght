

Snake1d {
	
	// things to do in order:
	// init game (check for connection to lightstuff, load sdefs, ...)
	// reset state to start
	// run! runrunrunrunrunrunrunrunrunrun...
	// game over: show points somehow? maybe scaled to the ring?
	
	
	var server;
	var runner; // holds routine to run game, stop with game.stop...
	var pixels = 20;
	
	var <>speed = 1;
	
	var asnake; // event with pixels, sdefs, length, pos
	var snakeDir; // direction, 1 or -1 (snake goes left or right)...
	var afruit; // position of a fruit
	var fruitSynth;
	var backgroundSynth;
	
	classvar <>channels = nil;
	classvar <>patcher = nil;
	
	*initClass {
		
	}
	
	*new { 
		^super.new.init();
	}
	init {
		// maybe check for lighting stuff here, i.e. which patcher to use, ...
		server = Server.default;
		
		if(channels.isNil, {
			"Set number of channels first!".error;
			^false;
		});
		if(patcher.isNil, {
			"Set patcher first!".error;
			^false;
		});
		
		
		runner = Routine.run({
			// load sdefs?
			this.loadSdefs();
			server.sync;
			
			this.initGame();
			server.sync;
			
			this.resetState();
			server.sync;
			
			this.runGame();
		});
	}
	
	loadSdefs {
		
		SynthDef(\snakedesert, { 
			var sig = (DC.kr(0)!3!channels).flatten;
			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});
		}).add;
		
		SynthDef(\snakehead, { |pos = 0|
			// pulsing türkis
			var color = Hsv2rgb.kr(2/6, 1, 1) * SinOsc.kr(2, 0, 0.15, 0.85);
			
/*			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});*/
			
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;

		SynthDef(\snakepixel, { |pos = 0|
			var color = Hsv2rgb.kr(3/6, 1, 0.5) * LFNoise0.kr(11.8, mul: 0.1, add: 1);			
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;

		SynthDef(\snakefruit, { |pos = 0|
			var color = Hsv2rgb.kr(0, 1, 1);
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;
		
		(
		SynthDef(\snakehit, { |pos = 0|
			
		}).add;
		)
	}
	
	initGame {
		asnake = (position: 0, size: 0, pixels: List(), synths: List());
		fruitSynth = Synth(\snakefruit);
		backgroundSynth = Synth(\snakedesert);
	}
	
	resetState {
		this.killSnake;
		server.sync;
		
		// create new snake
		snakeDir = [-1, 1].choose;
		
		asnake[\position] = pixels.rand;
		asnake[\size] = 2;
		asnake[\pixels].add(asnake[\position]);
		asnake[\pixels].add((asnake[\position] - snakeDir).wrap(0, channels-1));
		asnake[\synths].add(this.snakeHead(asnake[\pixels][0]));
		asnake[\synths].add(this.snakePixel(asnake[\pixels][1]));
		
		// create new fruit
		this.newFruit;
	}
	
	runGame {
		var eatenfruits = 0;
		inf.do({
			if(eatenfruits == 8, {
				this.resetState;
				eatenfruits = 0;
			});
			
			this.drawSnake;
			server.sync;
			
			// if snake ate a fruit
			if(asnake[\position] == afruit, {
				asnake[\size] = asnake[\size] + 1;
				this.newFruit;
				eatenfruits = eatenfruits+1;
				snakeDir = [-1, 1].choose;
				
			});
			
			// make a step
			this.snakeStep;
			
			(1/speed).wait;
		});
	}
	
	drawSnake {
		// basically just draws snake with its pixels/position
		// also adds new pixel if size has increased
		if(asnake[\size] > asnake[\pixels].size, {
			var newpixl = this.lastSnakePixel + (snakeDir * -1);
			asnake[\synths].add(this.snakePixel(newpixl));
			asnake[\pixels].add(newpixl);
		});
		
		asnake[\pixels].do({ |pos, i|
			asnake[\synths][i].set(\pos, pos);
			0.031.wait;
		});
	}
	
	snakeStep {
		// make a step for the whole snake...
		
		// add direction value and wrap!!!
		asnake[\position] = (asnake[\position] + snakeDir).wrap(0, channels-1);

		(asnake[\pixels].size-1).do({|i|
			asnake[\pixels][asnake[\pixels].size-1-i] = asnake[\pixels][asnake[\pixels].size-1-i-1];
		});
		
		asnake[\pixels][0] = asnake[\position];
	}
	
	newFruit {
		// new fruit pos at least 3 from snakepos away
		var newpos = asnake[\position] + ([-1, 1].choose * (3 + (channels - 6).rand));
		afruit = newpos.wrap(0, channels-1);
		fruitSynth.set(\pos, afruit);
	}
	
	snakeHead { |pos = 0|
		var syn = Synth(\snakehead, [\pos, pos]);
		^syn;
	}
	snakePixel { |pos = 0|
		var syn = Synth(\snakepixel, [\pos, pos]);
		^syn;
	}
	
	lastSnakePixel {
		// returns last pixel of a snake
		var size = asnake[\pixels].size;
		var pixel = asnake[\pixels][size-1];
		^pixel;
	}
	
	killSnake {
		asnake[\synths].size.do({ |i|
			asnake[\synths].removeAt(0).free;
		});
		asnake[\pixels] = List();
	}
	
	cleanUp {
		this.killSnake;
		fruitSynth.free;
		backgroundSynth.free;
	}
	
	stop {
		runner.stop.free;
		// clean up here...
		this.cleanUp;
	}
	
}