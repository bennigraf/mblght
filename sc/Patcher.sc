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
	var buffers; // holds DMXBuffer objects
	var oscfuncs;
	var <busses;
	var server; // holds default server since patcher uses busses!
	classvar <default; // default (usually first) Patcher...
	classvar <all; // holds all opened patchers for reference...
	
	*initClass {
		all = ();
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
	
	addBuffer { |buffer|
		// a buffer must react to the set method!
		buffers.add(buffer);
	}
	setBuffers { |dmxData, addr|
/*		"trying to set data to buffers:".postln;*/
/*		[dmxData, addr].postln;*/
		buffers.do({ |buf|
			buf.set(dmxData, addr);
		});
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
	makeRoutineForDevice { |device, buses|
		var routine = Routine.run({
			var val, lastval;
			inf.do({
				buses.keysValuesDo({ |method, bus|
					val = bus.getnSynchronous;
					if(val != lastval, {
						device.action(method, bus.getnSynchronous);
					});
					lastval = val;
				});
				this.setBuffers(device.getDmx, device.address);
				(1/30).wait;
			});
		});
		^routine;
	}
	
	
	removeDevice { |index|
		devices.removeAt(index);
	}
	
	addGroup { |groupname|
		if(groups.at(groupname.asSymbol).isNil, {
			groups.put(groupname.asSymbol, List());
		});
	}
/*	groups {*/
		
/*	}*/
	removeGroup { |group|
		if(group.isKindOf(Symbol), {
			groups.removeAt(groups.find([group]));
		});
		if(group.isKindOf(Integer), {
			groups.removeAt(group);
		});
	}
	addDeviceToGroup { |device, group|
		groups[group].add(device);
	}
	removeDeviceFromGroup { |device, group|
		groups[group].removeAt(group);
	}
	
	numDevices { |group = nil|
		if(group.isNil, {
			^devices.size;
		}, {
			^groups[group].size;
		});
	}
	
	busesForMethod { |method, deviceList|
		var buses = [];
		if(deviceList.isNil, {
			deviceList = devices;
		});
		deviceList.do({ |dev, i|
			if(dev.device.hasMethod(method), {
				buses = buses.add(dev.buses[method]);
			}, {
				buses = buses.add("false");
			});
		});
		^buses;
	}
	busesForGroupMethod { |group, method|
		var deviceList = groups[group];
		^this.busesForMethod(method, deviceList);
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
				deviceList[num % deviceList.size].buses[method].setn(data);
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
				(1/30).wait;
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
}