
/*
	DmxBuffer: Holds one Universe of Dmx data and sends it to various output devices, including Ola (pipe) 
	or Rainbowduino
	
	  buffer: buffers dmx data (512 channels of 8bit values)
	  devices: list of output devices. Object with .send method that receives complete universe...
*/
DmxBuffer {
	
	// some change...
	
	// instance vars
	var buffer;
	var <devices;
	var runner;
	
	var <>fps = 60; // fps to aim for
	
	classvar <knownDevices;
	
	*initClass {
		knownDevices = [OlaPipe, RainbowSerial];
	}
	
	*new {
		^super.new.init();
	}
	
	init {
		buffer = List.newClear(513).fill(0);
		devices = List();
		runner = this.makeRunner;
		runner.play;
	}
	
	close {
		devices.size.do({
			devices.pop.close;
		});
		runner.stop();
	}
	
	addDevice { |device|
		devices.add(device);
	}
	removeDevice { |index|
		if(devices[index].notNil, {
			devices[index].close;
			devices.removeAt(index);
		})
	}
	
	makeRunner {
		var routine = Routine({
			var time = thisThread.seconds;
			var newtime;
			
			// closure: count hits, give fps each /delta/ seconds
			var calcfps = { |delta = 5|
				var hits = 0;
				var waittime = delta;
				var lasttime = thisThread.seconds;
				var getfps = {
					hits = hits + 1;
					if(thisThread.seconds - lasttime  > waittime, {
						if(hits > 0, {
/*							("fps ca: "++(hits / waittime)).postln;*/
						}, {
/*							("fps < "++waittime++"!!").postln;*/
						});
						hits = 0;
						lasttime = thisThread.seconds;
					});
				};
				getfps;
			}.value();
			
			// main loop, send data to every device, wait a little to not lock up sc
			inf.do{ |i|
				calcfps.value();
				time = thisThread.seconds;
				devices.do({|dev|
					dev.send(buffer);
				});
				newtime = thisThread.seconds;
				if(newtime - time < 0.1, {
					// wait difference to 1/fps seconds to aim for certain frame rate
					((1/fps) - (newtime - time)).wait;
				}, {
					"frame rate problem!".postln;
/*					(1/fps).wait;*/
				});
			};
		});
		^routine;
	}
	
	set { |arg1 = nil, arg2 = nil|
		/// CHANGED: Offset for DMX where those idiots start to count from 1 instead of 0
		// 3 types:
		// a) set(channel, value)
		// b) set(list)
		// c) set(list, offset)
/*		"getting some data into buffer".postln;*/
/*		[arg1, arg2].postln;*/
		
		// a) set value at specific channel
		if(arg1.isKindOf(Integer) && arg2.isKindOf(Integer), {
			buffer[arg1] = arg2;
		});
		
		// b) + c) set list of values (optionally with offset)
		if(arg1.isKindOf(List), {
			var offset = 0;
			if(arg2.notNil, {
				offset = arg2;
			});
			arg1.do({ |val, i|
/*				buffer[i + offset + 1] = val;*/
				buffer[i + offset] = val;
			});
		})
	}
	
	get { |channel = nil|
		if(channel.notNil, {
			^buffer[channel];
		}, {
			^buffer
		});
	}
}



OlaPipe {
	/*
	Device for DmxBuffer
	Use Pipe to connect to olad and send data (using the .send method called by DmxBuffer)
	*/
	var pathToBin = "/usr/local/bin/ola_streaming_client";
	var <universe = 0;
	var pipe;
	
	
	*new { | myUniverse = 0|
		^super.new.init(myUniverse);
	}
	
	init { | myUniverse = 0 |
		universe = myUniverse;
		pipe = Pipe(pathToBin ++ " -u " ++ universe, "w");
	}
	close {
		pipe.close;
		pipe = nil;
	}
	
	send { | buffer |
		var datastring = "";
		buffer.do({ |obj, i| 
			datastring = datastring ++ obj.asString;
			if(i < (buffer.size - 1), {
				datastring = datastring ++ ",";
			});
		});
		datastring = datastring ++ "\n";
		if(pipe.notNil, {
			pipe.putString(datastring);
			pipe.flush;
		}, {
			"no pipe!".postln;
		});
	}
	
	describe { 
		// returns string that describes an instance of the object
		var str = "Universe: "++universe;
		^str;
	}
	
	compileString {
		var str = this.class.asCompileString++".new("++universe++")";
		^str;
	}
}


RainbowSerial {
	var sp; // holds serialport
	var lasttime; // to limit stuff to 20 fps to avoid overloading serial connection
	
	*new { |device|
		^super.new.init(device);
	}
	
	init { |device|
		if(device.isNil, {
			"select one of the following serial ports and give number".postln;
			SerialPort.devices.postln;
			^false;
		});
		if(device.isKindOf(Integer).not, {
			"give a number!".postln;
			^false;
		});
		("opening Port "++SerialPort.devices[device]).postln;
		sp = SerialPort(device, baudrate: 38400, crtscts: true);
		lasttime = 0;
	}
	close {
		sp.close;
	}
	
	send { |buffer|		
		var fbuf = Array.fill(192, 0); // buffer data here to convert to 8bit...
		var outData = Array.fill(192, 0);
		var realOutData = Int8Array.newClear(96);
		var byte1, byte2;
		var index = 0; // needed as manual counter...
		
		buffer.do({ |obj, i|
			if(i < 192, {
				fbuf[i] = obj;
			});
		});
		
		// considering buffer is filled with sequential rgb values for each 'lamp', we need to 
		// convert this to the buffer format used in rainbow: bbbb..gggg..rrrr..
		3.do({ |i|
			64.do({|j|
				outData[index] = fbuf[j * 3 + (2 - i)];
				index = index+1;
			});
		});
		
		// now we need to convert 8bit data to 2x4bit data
		96.do({ |i|
			byte1 = (outData[i*2].abs / 16).floor.asInteger << 4; // msb
			byte2 = (outData[i*2+1].abs / 16).floor.asInteger; // lsb
			realOutData[i] = (byte1 | byte2); // whew, that blew my mind...
		});
		
		
		// finally, put to port...
		if(sp.notNil, {
			// make sure we don't overload the serial connection and limit stuff to 20 fps for now
			if(thisThread.seconds - lasttime > (1/20), {
				lasttime = thisThread.seconds;
				sp.putAll(realOutData);
			});
		});
		
	}

	compileString {
		var str = this.class.asCompileString++".new(0)";
		^str;
	}
	
}