/*
	Patcher: Registers lighting equipment of certain types (?), manages routing to actual dmx addresses
		allows access to devices usign osc-like commands and methods, i.e.
			'/ring/1' (first device registered in 'ring')
			'/device/17' par 17...
			'/device/' all pars...
			'/ring' complete ring
		methods applicable to devices are defined in devices themselfs, i.e. color:
			'/par/17/color blue' or
			'/ring/3/color 33 127 244' (rgb) or
			'/ebene/strobo [1|0]' or other special functions
		that above doesn't work out. shit. new approach:
		
			'/patcherid/devices {n|type|all} {method} arg1 arg2 argn..'
			'/patcherid/groups {group} {method} arg1 arg2 argn..' // same as "all"
			'/patcherid/groups {group} {device n|type|all} {method} arg1 arg2 argn..'
			
 */
Patcher {
	var <devices;
	var <groups;
	var <id;
	var <buffers; // holds DMXBuffer objects
	var oscfuncs;
	var <busses;
	var server; // holds default server since patcher uses busses!
	var <>fps; // fps to get data from busses with
	classvar <default; // default (usually first) Patcher...
	classvar <all; // holds all opened patchers for reference...
	
	var aFun; // some vegas mode...
	
	*initClass {
		Class.initClassTree(Event);
		
		all = ();
		
		// default lighting event. Allows to play light Pattern style:
		// Pbind(\type, \light, \method, \dim, \data, Pwhite(0.1, 0.9, 5), \dur, 1).play
		Event.addEventType(\light, {
			var patcher;
			if(~patcher.isNil, {
				patcher = Patcher.default;
			}, {
				patcher = Patcher.all[~patcher];
			});
			//currentEnvironment.postln;
			if(patcher.notNil, {
				patcher.message(currentEnvironment);
			}, {
				"Patcher not reachable!".postln;
			});
		});
	}
	
	*new { |id|
		^super.new.init(id);
	}
	init { |myid|
/*		buffer = List.newClear(512).fill(0);*/
		devices = List();
		groups = IdentityDictionary();
		buffers = List();
		busses = List();
		server = Server.default;
		fps = 60;
		
		if(default==nil, { // make this the default patcher if none is there...
			default = this;
		});
		all.add(myid -> this);
		
		if(myid.isKindOf(Symbol).not, {
			"ID must be a symbol!".postln;
			^nil;
		});
		id = myid;
		
		server.waitForBoot();
		// deprecate osc functionality for now...
		/*
		oscfuncs = List();
		oscfuncs.add(OSCFunc.newMatching({
			("Patcher "++myid++" talking!").postln;
		}, '/'++myid));
		oscfuncs.add(OSCFunc.newMatching({ |msg| this.devicesMsg(msg) }, '/'++myid++'/devices'));
		oscfuncs.add(OSCFunc.newMatching({ |msg| this.groupsMsg(msg) }, '/'++myid++'/groups'));
		*/
	}
	
	// makes this patcher the default patcher
	makeDefault {
		default = this;
	}
	
	end {
		// frees buses, stop routines, remove devices?
		devices.do({ |dev|
			dev[\routine].stop;
			this.freeBusesForDevice(dev);
		});
		buffers.size.do({
			buffers.pop.close;
		});
		all.removeAt(id);
		if((default == this) && (all.size > 0), {
			default = all[all.keys.asArray.at(0)];
		});
	}
	
	addBuffer { |buffer|
		// a buffer must react to the set method!
		buffers.add(buffer);
	}
	setBuffers { |dmxData, addr|
/*		"trying to set data to buffers:".postln;*/
/*		[dmxData, addr].postln;*/
		buffers.do({ |buf|
			buf.set(dmxData, addr);
/*			buf.set(dmxData, addr - 1); // dmx starts at 1, everything else in the world at 0*/
		});
	}
	removeBuffer { |index|
		buffers.at(index).close();
		buffers.removeAt(index);
	}
	
	addDevice { |myDevice, myGroup|
		// add device to internal list of devices
		// register OSC path/address/methods? Or pass methods to Device... better not, otherwise 
		//   I have 5 methods for each device in memory... => call methods when device gets called!
		
		var deviceNum;
		var device = (); // holds device to add later
		var buses; // kr-buses used for data...
		var routine; // update-routine which calls actions periodically
		
		buses = this.makeBussesForDevice(myDevice);
		routine = this.makeRoutineForDevice(myDevice, buses);
		
		device[\device] = myDevice;
		device[\buses] = buses;
		device[\routine] = routine;
		
		devices.add(device);
		deviceNum = devices.size - 1;
		
		if(myGroup.notNil, {
			if(groups[myGroup].isNil, {
				groups.put(myGroup, List());
			});
			groups[myGroup].add(device);
		});
		
		// call init message as default...
		if(myDevice.hasMethod(\init), {
			myDevice.action(\init);
			this.setBuffers(myDevice.getDmx, myDevice.address);
		});
		
		// create busses for each method, get their data in a routine or something...
	}
	makeBussesForDevice { |myDevice|
		// make bus for every method, store in busses-List
		var buses = Dictionary(); // key->value list
		// somewhereHere...
		var reservedKeys = ['channels', 'init', 'numArgs'];
		Device.types[myDevice.type].keysValuesDo({ |method|
			// do for each method, but omit reserved keys 'channel', 'init', 'numArgs:
			if(reservedKeys.includes(method) == false, {
				var numArgs = Device.types[myDevice.type].numArgs[method];
				buses.put(method, Bus.control(server, numArgs));
			});
		});
		^buses;
	}
	freeBusesForDevice { |myDevice|
		myDevice[\buses].do({ |bus|
			bus.free;
		});
	}
	makeRoutineForDevice { |device, buses|
		var routine = Routine.run({
			var val, lastval;
			inf.do({
				var changed = false;
				buses.keysValuesDo({ |method, bus|
					val = bus.getnSynchronous;
					if(val != lastval, {
						device.action(method, val);
						changed = true;
					});
					lastval = val;
				});
				if(changed, {
					this.setBuffers(device.getDmx, device.address);
				});
				(1/fps).wait;
			});
		});
		^routine;
	}
	
	removeDevice { |index|
		groups.keysValuesDo({ |grpname, devices|
			devices.do({ |dev, n|
				if(dev == devices[index], {
					this.removeDeviceFromGroup(n, grpname);
				});
			});
		});
		devices[index][\routine].stop;
		this.freeBusesForDevice(devices[index]);
		devices.removeAt(index);
	}
	
	nextFreeAddr { |numChans = 1|
		var chans = nil!512;
		var freeChan = nil;
		var cntr, n;
		devices.do({ |dev|
			var channels = Device.types.at(dev.device.type).at(\channels);
			var address = dev.device.address.asInteger;
			for(address, (address + channels - 1), { |n|
				chans[n] = 1;
			});
		});
		// 2nd approach: Step through chans, run counter that adds up if channel is free and otherwise resets, once counter = numChans -> found free slot!
		cntr = 0;
		n = 0;
		while({(cntr < numChans) && (n < 512)}, {
			if(chans[n].isNil, {
				cntr = cntr + 1;
			}, {
				cntr = 0;
			});
			n = n + 1;
		});
		if(cntr == numChans, {
			// found one!
			freeChan = n - numChans;
		});
		^freeChan;
	}
	
	
	groupNames { 
		var names = [];
		groups.keysValuesDo({ |name|
			names = names.add(name)
		});
		^names;
	}
	addGroup { |groupname|
		if(groups.at(groupname.asSymbol).isNil, {
			groups.put(groupname.asSymbol, List());
		});
	}
	removeGroup { |group|
		if(group.isKindOf(Symbol), {
			groups.removeAt(group);
		});
		if(group.isKindOf(Integer), {
			"fix me!".postln;
/*			groups.removeAt(group);*/
		});
	}
	addDeviceToGroup { |device, group|
		groups[group].add(device);
	}
	removeDeviceFromGroup { |deviceIndx, group|
		groups[group].removeAt(deviceIndx);
	}
	
	numDevices { |group = nil|
		if(group.isNil, {
			^devices.size;
		}, {
			^groups[group].size;
		});
	}
	
	busesForMethod { |method, deviceList|
		var buses = List();
		if(deviceList.isNil, {
			deviceList = devices;
		});
		deviceList.do({ |dev, i|
			if(dev.device.hasMethod(method), {
				buses.add(dev.buses[method]);
			});
		});
		^buses;
	}
	numBusesForMethod{ |method, deviceList|
		var numbuses = 0;
		if(deviceList.isNil, {
			deviceList = devices;
		});
		deviceList.do({ |dev, i|
			if(dev.device.hasMethod(method), {
				numbuses = numbuses +1;
			});
		});
		^numbuses;
	}
	busesForGroupMethod { |group, method|
		var deviceList = groups[group];
		^this.busesForMethod(method, deviceList);
	}
	numBusesForGroupMethod { |group, method|
		var deviceList = groups[group];
		^this.numBusesForMethod(method, deviceList);
	}
	
	
	message { |msg|
		// dispatches message, calls methods on devices, sends dmx data to buffer 
		// possible message addresses:
		//   group: /{patcher}/group {method} - call method on each device in group
		//   group: /{patcher}/group {n} {method} - call method on {n}'th device in group
		//   device: /{patcher}/devices {method} - call method on every deivce in patcher (which supports this specific method)
		//   device: /{patcher}/devices {n} {method} - call method on {n}'th device in patcher
		//   patcher: /{method} - call method on every device in patcher, same as /device/{method}
		/*
		 * OR:
		 * event-messages:
		msg = (
			group: \ring,
			method: \color,
			data: [24, 34, 12]
		);
		e = ()
		e[\play].def.sourceCode
		
		Patcher.message(msg); => dispatch to group/device, call often!
		
		*/
		if(msg[\group] != nil, {
/*			"make group message".postln;*/
			this.groupsMsgEvent(msg);
		}, {
			// otherwise make device message, which calls message on all devices if none is given
/*			"make device message".postln;*/
			this.devicesMsgEvent(msg);
		});
		
	}
	
	devicesMsgEvent { |msg, deviceList = nil|
		var deviceNums = msg[\device]; // can be array...
		var method = msg[\method];
		var data = msg[\data];

		// 'default' device list are the devices of the patcher
		if(deviceList == nil, {
			deviceList = devices;
		});
		
		if(deviceNums == nil, {
			// apply to all devices in patcher
			deviceNums = (0..(deviceList.size-1))
		});
		if(deviceNums.isKindOf(Array).not, {
			deviceNums = [deviceNums];
		});
		deviceNums.do({ |num, i|
			if(deviceList[num % deviceList.size].device.hasMethod(method), {
				// wrap devices index, just to be sure...
/*				deviceList[num % deviceList.size].action(method, data);*/
/*				this.setBuffers(deviceList[num%deviceList.size].getDmx, deviceList[num%deviceList.size].address);*/
				// rewrite: write data to bus instead of device directly.
				if(data.isKindOf(Array), {
					deviceList[num % deviceList.size].buses[method].setn(data);
				}, {
					deviceList[num % deviceList.size].buses[method].set(data);
				});
			});
		});
	}
	groupsMsgEvent { |msg|
		// reroute call to devicesmsgevent, but with 'filtered' list of devices...
		var group = msg[\group];
		var groupDevices = groups[group];
		this.devicesMsgEvent(msg, groupDevices);
	}
	
	// basically OSCFunc callbacks... get: msg, time, addr, and recvPort
	// a little deprecated!
	devicesMsg { |msg, time, addr, recvPort|
		// msg[0] is address, msg[1] and following are arguments
/*		msg.postln;*/
/*		msg.do({ |d| d.class.postln });*/
		if(msg[1].isKindOf(Integer), {
			var deviceNum = msg[1];
			var method = msg[2];
			var arguments = List();
			(msg.size-3).do({ |i|
				arguments.add(msg[i + 3]);
			});
/*			[method, arguments].postln;*/
			if(deviceNum < devices.size, {
				devices[deviceNum].action(method, arguments);
				this.setBuffers(devices[deviceNum].getDmx, devices[deviceNum].address);
			}, {
				"device doesn't exist in patcher!".postln;
			});
		}, { // else: method called on all devices
			var method = msg[1];
			var arguments = List();
			(msg.size-2).do({ |i|
				arguments.add(msg[i + 2]);
			});
			devices.do({ |device, i|
				if(device.hasMethod(method), {
					device.action(method, arguments);
					this.setBuffers(device.getDmx, device.address);
				});
			});
		});
	}
	
	groupsMsg {|msg, time, addr, recvPort|
/*		msg.postln;*/
		var group = msg[1];
		var groupDevs = groups[group];
		var method = msg[2];
		var arguments = List();
		(msg.size-3).do({ |i|
			arguments.add(msg[i + 3]);
		});
		groupDevs.do({|dev, i|
			if(dev.hasMethod(method), {
				dev.action(method, arguments);
				this.setBuffers(dev.getDmx, dev.address);
			});
		});
	}
	
	
	// now a patcher gets NodeProxys registered for methods. Those NodeProxy should play out
	// .kr signals, whose values are being used to call the registered methods on the certain
	// device or group of devices.
	// Since a method might require multiple arguments, multichannel busses are needed. There
	// is no way of having actual groups of busses or something like that, so there must be 
	// arguments * channels busses available (in case there are different values for different 
	// devices). The devices wrap around the available devices in any certain group.
	
	// Or: a patcher registers busses for methods. Then NodeProxys need to play to those busses.
	// (A NodeProxy with Out.kr appareantly doesn't even create it's own private bus, see NP.busLoaded.)
	// deprecated!!
	makeBusForMethod { |method, numArgs = 1, group = nil, channels = 1|
		
		var bus = (); // bus proto...
		var numDevices = this.numDevices(group);
		// how do I get the number of arguments for a method??? I don't! => numArgs...
		
		if(server.pid == nil, {
			"Boot server first!!".postln;
			^false;
		});
		
		if(group.notNil, {
			bus[\group] = group;
		});
		
		bus.numArgs = numArgs;
		bus.channels = channels; // notice that bus.channels != bus.bus.numChannels, the latter is channels*numArgs!
		bus.method = method;
		
		bus.bus = Bus.control(server, numArgs * channels);
		
		bus.routine = Routine.run({
			var busdata;
			var message = ();
			inf.do({
				message[\method] = bus.method;
				if(bus.group.notNil, {
					message[\group] = group;
				});
				// wrap around things? hmmm...
				// if there is 1 channel, call on any device. if there are >1 channels, call on 
				// each device 
				if(bus.channels == 1, {
					// bus contains only data for 1 channel so it also must be numArgs big...
					message[\data] = bus.bus.getnSynchronous;
/*					message.postln;*/
					this.message(message);
				}, {	
					// for each device get data from bus (!offset!), wrap bus channels...
					busdata = bus.bus.getnSynchronous; // .getnAt doesn't exist...
					numDevices.do({ |i|
						var offset = i*bus.numArgs;
						message[\device] = i;
						// wrapAt with an array gives array of values at indizies given by array
						message[\data] = busdata.wrapAt((offset..(offset+numArgs-1)));
						this.message(message);
					});
				});
				(1/fps).wait;
			});
		});
		
		// add bus to bus-dictionary
		busses.add(bus);
	}
	removeBus { |index|
		if (busses[index].notNil, {
			busses[index].routine.stop;
			busses[index].bus.free;
			busses.removeAt(index);
		}, {
			("Nothing found at index "+index).postln;
		});
	}
	
	
	havefun { |group = 'stage'|
		"Fun".postln;
		if(aFun.notNil, { aFun.free });
		aFun = {
			var buses = this.busesForMethod(\color);
			var point1 = LFNoise1.kr(8.3/120).range(0, 4).fold(0, 1).lag3(0.5);
			var point2 = LFNoise2.kr(7.2/130).range(0, 4).fold(0, 1).lag3(0.5);
			buses.do({ |bus, n|
				var position = 1/buses.size * n;
				var distance = (position - [point1, point2]).abs;
				var sins = SinOsc.kr(0, distance[0] * 2pi, 0.5, 0.5)
						+ SinOsc.kr(0, distance[1] * 2pi, 0.5, 0.5)
						+ SinOsc.kr(0, distance.sum * pi)
						+ SinOsc.kr(0, distance.sum / 2 * pi + LFTri.kr(1/18.83), 0.4)
							/ 3.6;
				var color = Hsv2rgb.kr(sins.fold(0, 1), 1, 1).lag3(2);
				Out.kr(bus, color);
			});
			0;
		}.play;
	}
	enoughfun {
		var black;
		aFun.free;
		black = {
			var buses = this.busesForMethod(\color);
			var color = [0, 0, 0];
			buses.do({ |bus, n|
				Out.kr(bus, color);
			});
			EnvGen.kr(Env.perc(1), doneAction: 2);
		}.play;
		aFun = nil;
	}
}