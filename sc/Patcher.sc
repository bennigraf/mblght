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
	
	
	*new { |id|
		^super.new.init(id);
	}
	init { |myid|
/*		buffer = List.newClear(512).fill(0);*/
		devices = List();
		groups = IdentityDictionary();
		buffers = List();
		if(myid.isKindOf(Symbol).not, {
			"ID must be a symbol!".postln;
			^nil;
		});
		id = myid;
		oscfuncs = List();
		
		oscfuncs.add(OSCFunc.newMatching({
			("Patcher "++myid++" talking!").postln;
		}, '/'++myid));
		oscfuncs.add(OSCFunc.newMatching({ |msg| this.devicesMsg(msg) }, '/'++myid++'/devices'));
		oscfuncs.add(OSCFunc.newMatching({ |msg| this.groupsMsg(msg) }, '/'++myid++'/groups'));
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
	
	addDevice { |device, group|
		// add device to internal list of devices
		// register OSC path/address/methods? Or pass methods to Device... better not, otherwise 
		//   I have 5 methods for each device in memory... => call methods when device gets called!
		devices.add(device);
		
		if(group.notNil, {
			if(groups.at(group).isNil, {
				groups.put(group, List());
			});
			groups.at(group).add(device);
		});
	}
/*	devices {*/
		
/*	}*/
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
	
	
	message { |msg|
		// dispatches message, calls methods on devices, sends dmx data to buffer 
		// possible message addresses:
		//   group: /{patcher}/group {method} - call method on each device in group
		//   group: /{patcher}/group {n} {method} - call method on {n}'th device in group
		//   device: /{patcher}/devices {method} - call method on every deivce in patcher (which supports this specific method)
		//   device: /{patcher}/devices {n} {method} - call method on {n}'th device in patcher
		//   patcher: /{method} - call method on every device in patcher, same as /device/{method}

		// nope, doing osc...
	}
	
	// basically OSCFunc callbacks... get: msg, time, addr, and recvPort
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
	
}


/*
	Provides methods to use devices without knowing their exact addresses...
	holds it's own little buffer for it's own dmx data, accessible via setDmx, info gets called from patcher
 */
Device {	
	classvar types; // holds different types of devices
	var <>address = 0;
	var <>type;
	var <>dmxData;
	
	*initClass {
		// load some default devices on class instantiation
		Device.addType(\smplrgbpar, (
			channels: 3,
			color: { |args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				[[0, args[0]], [1, args[1]], [2, args[2]]];
			}
		));
	}
	
	*new { |type, address = 0|
		^super.new.init(type, address);
	}
	init { | type, address = 0 |
		var channels;
		this.address = address;
		this.type = type;
		channels = types[type][\channels];
		this.dmxData = List.newClear(channels).fill(0);
	}
	
	*addType { |title, definition| 
		if(types.isNil, {
			types = IdentityDictionary();
		});
		if(title.isKindOf(Symbol).not, {
			"Give a symbol as device title!".postln;
			^false;
		});
		if(definition.isKindOf(Event).not, {
			"Give an event as definition...".postln;
			^false;
		});
		types.put(title.asSymbol, definition);
		// give default channel count...
		if(definition.at(\channels)==nil, {
			types.at(title.asSymbol)[\channels] = 1;
		});
	}
	*types {
		if(types.isNil, {
			types = IdentityDictionary();
		});
		^types;
	}
	
	setDmx { |addr, value|
		// set dmx data locally! use internal mini pseudo buffer
		dmxData[addr] = value;
	}
	getDmx {
		^dmxData;
	}
	
	action { |method, arguments|
		// get type definition wiht methods from global type dictionary stored in classvar types
		var def = types.at(type);
		if(def.at(method.asSymbol).notNil, {
			var tmpDmxData = def.at(method.asSymbol).value(arguments);
/*			("calling method "++method).postln;*/
			tmpDmxData.do({ |chan|
				this.setDmx(chan[0], chan[1]);
			});
		}, {
			"method not found!".postln;
		});
	}
	
	hasMethod { |method|
		var def = types.at(type);
		^def.at(method.asSymbol).notNil;
	}
}

/*
	Example for definition of device. Implement osc methods by setting right values to the right dmx channels.
		addr is used as starting address of device here.
		TODO: scaling of values?
 */

/*
(
Device.addType(\rgbpar, (
	channels: 5,
	color: { |args|
//		var r = args[0];
//		var g = args[1];
//		var b = args[2];
//		this.setDmx(addr, r);
//		this.setDmx(addr+1, g);
//		this.setDmx(addr+2, b);
		// return list with dmx slots/addresses (starting from 0 for this device) and values
		[[0, args[0]], [1, args[1]], [2, args[2]]];
	},
	strobe: { |onoff|
		if(onoff == "on", {
			this.setDmx(this.addr+4, 255);
		}, {
			this.setDmx(this.addr+4, 0);
		})
	}
));
)
*/

/*
// later I would say:
p = Patcher();
p.addDevice(Device(\rgbpar), 17); // 17 is the starting address of the rgbpar I add here...
p.addGroup('ring'); // creates a 'ring'
p.addToGroup('ring', p.devices[0]); // add first device to group ring
p.message('/ring/0/color 255 0 0');

*/