
/*
	DmxBuffer: Holds Dmx data and sends it to various output devices, including Ola (pipe)
	or Rainbowduino
	A device usually is a dmx universe (512 channels), the buffer can hold more than that!

	  buffer: buffers dmx data (512 channels of 8bit values per universe)
	  devices: list of output devices. Object with .send method that receives complete universe...
*/

// DmxBuffer is deprecated! Don't use me! (But still use the output devices below....)
DmxBuffer {

	// some change...

	// instance vars
	var <buffer;
	var <devices;
	var runner;
	var <numUniverses;
	var universeList;

	var <>fps = 40; // fps to aim for

	classvar <knownDevices;

	*initClass {
		knownDevices = [OlaPipe, RainbowSerial, OlaOsc, GenOsc, OscNoBlob];
	}

	*new { | mNumUniverses = 1|
		^super.new.init(mNumUniverses);
	}

	init { | mNumUniverses = 1|
		numUniverses = mNumUniverses;
		buffer = List.newClear(512 * this.numUniverses).fill(0);
		devices = List();
		universeList = List();
		runner = this.makeRunner;
		runner.play;
	}

	close {
		devices.size.do({
			devices.pop.close;
		});
		runner.stop();
	}

	addDevice { |device, universe = 0|
		devices.add(device);
		universeList.add(universe);
	}
	removeDevice { |index|
		if(devices[index].notNil, {
			devices[index].close;
			devices.removeAt(index);
		});
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
				devices.do({|dev, i|
					var u = universeList[i];
					if(u + 1 * 512 - 1 < buffer.size, {
						dev.send(buffer.copyRange(512 * u, 512 * (u + 1) - 1));
					});
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
			if(arg1 < buffer.size, {
				buffer[arg1] = arg2;
			});
		});

		// b) + c) set list of values (optionally with offset)
		if(arg1.isKindOf(List), {
			var offset = 0;
			if(arg2.notNil, {
				offset = arg2;
			});
			arg1.do({ |val, i|
/*				buffer[i + offset + 1] = val;*/
				if(i + offset < buffer.size, {
					buffer[i + offset] = val;
				});
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
		if(pipe.notNil, {
			pipe.close;
			pipe = nil;
		});
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


OlaOsc {
	/*
	Device for DmxBuffer
	Send to OLA using the newly adapted OSC option
	*/
	var <universe = 0;
	var net;
	var channelLimit = 512;


	*new { | myUniverse = 0, myChanLimit = 512 |
		^super.new.init(myUniverse, myChanLimit);
	}

	init { | myUniverse = 0, myChanLimit = 512 |
		universe = myUniverse;
		channelLimit = myChanLimit;
		net = NetAddr.new("127.0.0.1", 7770)
	}
	close {
		if(net.notNil, {
/*			net.close;*/
			net = nil;
		});
	}

	send { | buffer |
		var data = Int8Array.newFrom(buffer);

		if (channelLimit < 512, {
			data = data.copyRange(0, channelLimit - 1);
		});
		if(net.notNil, {
			net.sendMsg(("/dmx/universe/"++universe).asSymbol, data);
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

MicroOla {
	/*
	Device for DmxBuffer
	Send to OLA using the newly adapted OSC option
	*/
	var <universe = 0;
	var net;
	var chancount;

	*new { | myUniverse = 0, myChancount = 16|
		^super.new.init(myUniverse, myChancount);
	}

	init { | myUniverse = 0, myChancount = 16|
		universe = myUniverse;
		chancount = myChancount;

		net = NetAddr.new("127.0.0.1", 7770)
	}
	close {
		if(net.notNil, {
/*			net.close;*/
			net = nil;
		});
	}

	send { | buffer |
		var data = Int8Array.newFrom(buffer);
		if(net.notNil, {
			chancount.do({ |n|
				net.sendMsg(("/dmx/universe/"++universe).asSymbol, n+1, buffer[n]);
			});
		});
	}

	describe {
		// returns string that describes an instance of the object
		var str = "Universe: "++universe++", channels: "++chancount;
		^str;
	}

	compileString {
		var str = this.class.asCompileString++".new("++universe++", "++chancount++")";
		^str;
	}
}



GenOsc {
	/*
	Device for DmxBuffer
	Generic OSC interface, kind of...
	TODO: set ip-address, maybe as string like 127.0.0.1:12345/dmx/universe or so...
	*/
	var <addr = "127.0.0.1";
	var <path = '/dmx';
	var <port = 13335;
	var net;


	*new { | myAddr = nil, myPath = nil, myPort = nil|
		^super.new.init(myAddr, myPath, myPort);
	}

	init { | myAddr, myPath, myPort|
		if(myPath.isNil, {
			myPath = '/dmx';
		});
		if(myPort.isNil, {
			myPort = 13335
		});
		path = myPath;
		port = myPort;
		addr = myAddr;
		net = NetAddr.new(addr, port)
	}
	close {
		if(net.notNil, {
/*			net.close;*/
			net = nil;
		});
	}

	send { | buffer |
		var data = Int8Array.newFrom(buffer);
		if(net.notNil, {
			net.sendMsg(path.asSymbol, data);
		});
	}

	describe {
		// returns string that describes an instance of the object
		var str = "Path: "++path++", Port: "++port;
		^str;
	}

	compileString {
		var str = this.class.asCompileString++".new('"++path++"', "++port++")";
		^str;
	}
}

OscNoBlob {
	/*
	Device for DmxBuffer
	Generic OSC interface, but send values as integers, not as big blob (slower but libcinder wants that)
	TODO: set ip-address, maybe as string like 127.0.0.1:12345/dmx/universe or so...
	*/
	var <path = '/dmx';
	var <port = 13335;
	var net;


	*new { | myPath = nil, myPort = nil|
		^super.new.init(myPath, myPort);
	}

	init { | myPath, myPort|
		if(myPath.isNil, {
			myPath = '/dmx';
		});
		if(myPort.isNil, {
			myPort = 13335
		});
		path = myPath;
		port = myPort;
		net = NetAddr.new("127.0.0.1", port)
	}
	close {
		if(net.notNil, {
/*			net.close;*/
			net = nil;
		});
	}

	send { | buffer |
		// var data = Int8Array.newFrom(buffer);
		if(net.notNil, {
			net.sendMsg(path.asSymbol, *buffer.keep(512)); // * unpacks data to avoid sending a blob
		});
	}

	describe {
		// returns string that describes an instance of the object
		var str = "Path: "++path++", Port: "++port;
		^str;
	}

	compileString {
		var str = this.class.asCompileString++".new('"++path++"', "++port++")";
		^str;
	}
}



SmplSerial {
	var port, lastUpdate;

	*new { |device, bauds = 9600|
		^super.new.init(device, bauds);
	}

	init { |device, bauds|
		if(device.isNil, {
			"select one of the following serial ports and give number".postln;
			SerialPort.devices.postln;
			^false;
		});
		("opening Port " ++ device).postln;
		port = SerialPort.new(device, bauds);
		port.isOpen.not.if({
			"port didnt open!".postln;
			^false;
		});
		lastUpdate = 0;
	}
	close {
		port.close;
	}

	send { |buffer|
		//(thisThread.seconds - lastUpdate > (1/30) && port.isOpen).if({
    port.isOpen.if({
			// there seems to be a limit, I guess because of some serial buffer
			var str = buffer.keep(100).join(",");

			port.putAll(str ++ "\n");

			// ("put data(" ++ port.isOpen ++ ", " ++buffer.size++"): " ++ str).postln;
			lastUpdate = thisThread.seconds;
		})
	}

	compileString {
		var str = this.class.asCompileString++".new(0)";
		^str;
	}
}




// connect to rainbowduinoboard with 8x8 rgb matrix on it running "firmwre_4bit"
// (see DirectMode, Rainbowduino Dashboard, ...)
RainbowSerial {
	var sp; // holds serialport
	var lasttime; // to limit stuff to 20 fps to avoid overloading serial connection

	*new { |device, bauds = 28800|
		^super.new.init(device, bauds);
	}

	init { |device, bauds|
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
		sp = SerialPort(device, baudrate: bauds, crtscts: true);
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