
/*
	DmxBuffer: Holds one Universe of Dmx data and sends it to various output devices, including Ola (pipe) 
	or Rainbowduino
	
	  buffer: buffers dmx data (512 channels of 8bit values)
	  devices: list of output devices. Object with .send method that receives complete universe...
*/
DmxBuffer {
	
	// classvars
	classvar x = 0;
	
	// some change...
	
	// instance vars
	var buffer;
	var devices;
	var runner;
	
	var >fps = 20; // fps to aim for
	
	// class methods
	*classmethoda {
		
	}
	
	
	// instance methods 
	instancemethodb {
		
	}
	
	
	*new {
		^super.new.init();
	}
	
	init {
		buffer = List.newClear(512).fill(0);
		devices = List();
		runner = this.makeRunner;
		runner.play;
	}
	
	addDevice { |device|
		devices.add(device);
	}
	devices { 
		^devices;
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
							("fps ca: "++(hits / waittime)).postln;
						}, {
							("fps < "++waittime++"!!").postln;
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
				});
			};
		});
		^routine;
	}
	
	set { |arg1 = nil, arg2 = nil|
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
				buffer[i + offset] = val;
			});
		})
	}
	
	get { |channel|
		^buffer[channel];
	}
}



OlaPipe {
	/*
	Device for DmxBuffer
	Use Pipe to connect to olad and send data (using the .send method called by DmxBuffer)
	*/
	var pathToBin = "/usr/local/bin/ola_streaming_client";
	var <>universe = 0;
	var pipe;
	
	*new {
		^super.new.init();
	}
	
	init {
		pipe = Pipe(pathToBin ++ " -u " ++ universe, "w");
	}
	close {
		pipe.close;
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
}


RainbowSerial {
	var sp; // holds serialport
	
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
		
	}
	close {
		sp.close;
	}
	
	send { |buffer|		
		var fbuf = Int8Array.newClear(192); // buffer data here to convert to 8bit...
		var outData = Int8Array.newClear(192);
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
			sp.putAll(realOutData);
		});
		
	}
	
}