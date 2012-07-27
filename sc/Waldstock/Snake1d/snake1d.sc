

Snake1d {
	
	// things to do in order:
	// init game (check for connection to lightstuff, load sdefs, ...)
	// reset state to start
	// run! runrunrunrunrunrunrunrunrunrun...
	// game over: show points somehow? maybe scaled to the ring?
	
	
	var server;
	var runner; // holds routine to run game, stop with game.stop...
	var pixels = 20;
	
	var asnake; // event with pixels, sdefs, length, pos
	var snakeDir; // direction, 1 or -1 (snake goes left or right)...
	var afruit; // position of a fruit
	var fruitSynth;
	
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
		SynthDef(\snakehead, { |pos = 0|
			// pulsing türkis
			var color = Hsv2rgb.kr(4.5/6, 1, 1) * SinOsc.kr(2, 0, 0.25, 0.75);
			
/*			Patcher.all[patcher].busesForGroupMethod(\ring, \color).do({ |bus, i|
				Out.kr(bus, [sig[i*3], sig[i*3+1], sig[i*3+2]])
			});*/
			
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;

		SynthDef(\snakepixel, { |pos = 0|
			var color = Hsv2rgb.kr(4.5/6, 1, 0.8);			
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;

		SynthDef(\snakefruit, { |pos = 0|
			var color = Hsv2rgb.kr(LFNoise0.kr(13).range(0, 1), 1, 1);
			var buses = Patcher.all[patcher].busesForGroupMethod(\ring, \color);
			Out.kr(Select.kr(pos, buses), color);
		}).add;
	}
	
	initGame {
		asnake = (position: 0, size: 0, pixels: List(), synths: List());
		fruitSynth = Synth(\snakefruit);
	}
	
	resetState {
		this.killSnake;
		
		// create new snake
		snakeDir = [-1, 1].choose;
		
		asnake[\position] = pixels.rand;
		asnake[\size] = 2;
		asnake[\synths].add(this.snakeHead((asnake[\position] + snakeDir).wrap(0, channels-1)));
		asnake[\synths].add(this.snakePixel(asnake[\position]));
		
		// create new fruit
		this.newFruit;
	}
	
	runGame {
		inf.do({
			this.drawSnake;
			server.sync;
			
			// if snake ate a fruit
			if(asnake[\position] == afruit, {
				asnake[\size] = asnake[\size] + 1;
				this.newFruit;
			});
			
			// make a step
			this.snakeStep;
			
			1.wait;
		});
	}
	
	drawSnake {
		// basically just draws snake with its pixels/position
		// also adds new pixel if size has increased
		if(asnake[\size] > asnake[\synths].size, {
			asnake[\synths].add(this.snakePixel(this.lastSnakePixel + snakeDir));
		});
		
		asnake[\pixels].do({ |pos, i|
			asnake[\synths][i].set(\pos, pos);
			0.1.wait;
		});
	}
	
	snakeStep {
		// make a step for the whole snake...
		
		// add direction value and wrap!!!
		asnake[\position] = (asnake[\position] + snakeDir).wrap(0, channels-1);
		
		asnake[\pixels][0] = asnake[\position];
		(asnake[\pixels].size-1).do({|i|
			asnake[\pixels][i+1] = asnake[\pixels][i];
		});
	}
	
	newFruit {
		// new fruit pos at least 3 from snakepos away
		var newpos = asnake[\pos] + ([-1, 1].choose * (3 + (channels - 6).rand));
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
		asnake[\synths].do({ |synth, i|
			synth.free;
		});
		asnake[\pixels] = List();
	}
	
	cleanUp {
		this.killSnake;
		fruitSynth.free;
	}
	
	stop {
		runner.stop.free;
		// clean up here...
		this.cleanUp;
	}
	
}