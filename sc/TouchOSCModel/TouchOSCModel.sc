TouchControl {
	
	var server; // holds server to run control bus on
	var bus; // holds bus of this control
	var <hasBus; // bool this control has a bus
	var addr; // holds osc addr
	var type; // fader, button, toggle, xy, multifader, multixy, label
	
	var <val; // holds actual value of control
	var <>action; // an action function
	var responder; // holds OSCFunc which takes values
	var tracer; // routine that updates controller with values from bus
	var traceNode; // a node that runs a sendreply on the server to get bus values
	var traceResp; // a OSCFunc "osc proxy" that responds to sendreply on server and sends values on to TouchOSC
	
	classvar <>recvAddr, <>replAddr;
	
	classvar <types;
	
	*initClass {
		types = (
			fader: (busChannels: 1),
			multifader: (), // set busChannels in arguments...
			label: (busChannels: 1), // well, ignore this, bad coding here...
			button: (busChannels: 1),
			toggle: (busChannels: 1)
		);
	}
	
	*new { |type, name, oscaddr, makeBus, arguments|
		^super.new.init(type, name, oscaddr, makeBus, arguments);
	}
	
	init { |myType, oscaddr, makeBus, arguments|
		type = myType ?? \button;
		addr = oscaddr ?? '/test';
		makeBus = makeBus ?? true;
		server = Server.default;
		
		bus = nil;
		val = nil;
		action = { };
		hasBus = false;
		
		// set some initial arguments externally if needed, i.e. num of faders for multifader
		this.initType(arguments); 
		
		if(makeBus, {
			hasBus = true;
			server.waitForBoot({
				bus = Bus.control(server, types[type][\busChannels]);
				server.sync;
				if(replAddr.class == NetAddr, {
/*					tracer = this.makeTraceRoutine;*/
					this.makeTraceStuff;
				});
			});
		});
		
		responder = this.makeOscResponder();
	}
	
	initType { |arguments|
		if(type == \multifader, {
			types[\multifader][\busChannels] = arguments;
		});
	}
	
	bus {
		if(bus.notNil, {
			^bus;
		}, {
			"This control doesn't have a bus!".error;
			^nil;
		});
	}
	
	set { |aval = 0, runAction = true|
		val = aval;
		if(hasBus, {
			if(val.isArray, {
				bus.setnSynchronous(val);
			}, {
				bus.setSynchronous(val);
			});
		});
		if(runAction, {
			action.value(val);
		});
		if(replAddr.class == NetAddr, {
			replAddr.sendMsg(addr, *val); // * unpacks array here...
		});
	}
	
	makeOscResponder { 
		var f;
		if(types[type][\busChannels]>1, {
			f = [];
			types[type][\busChannels].do({ |n|
				f = f.add(OSCFunc({ |msg|
					if(hasBus, {
						bus.setAt(n, msg[1]);
					});
					val[n] = msg[1];
					action.value(n, msg[1]);
				}, addr++"/"++(n+1), recvAddr));
			});
		}, {
			f = OSCFunc({ |msg|
				if(hasBus, {
					bus.setSynchronous(msg[1]);
				});
				val = msg[1];
				action.value(msg[1]);
			}, addr, recvAddr);
		});
		^f;
	}
	
	makeTraceStuff {
		traceNode = {
			var trig = Impulse.kr(25);
			SendReply.kr(trig, "/busvalue"++addr, In.kr(bus, types[type][\busChannels]));
		}.play;
		traceResp = OSCFunc({ |msg|
			var myMessage = msg[3..]; // this magic is .copyRange!
			replAddr.sendMsg(addr, *myMessage); // The star unpacks the array somehow
		}, '/busvalue'++addr, recvAddr);
	}
	
}