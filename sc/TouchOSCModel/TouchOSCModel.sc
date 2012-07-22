TouchControl {
	
	var server; // holds server to run control bus on
	var bus; // holds bus of this control
	var hasBus; // bool this control has a bus
	var addr; // holds osc addr
	
	var <val; // holds actual value of control
	var <>action; // an action function
	var responder; // holds OSCFunc which takes values
	var tracer; // routine that updates controller with values from bus
	var traceNode; // a node that runs a sendreply on the server to get bus values
	var traceResp; // a OSCFunc "osc proxy" that responds to sendreply on server and sends values on to TouchOSC
	
	classvar <>recvAddr, <>replAddr;
	
	*initclass {
		
	}
	
	*new { |type, name, oscaddr, makeBus|
		^super.new.init(type, name, oscaddr, makeBus);
	}
	
	init { |myType, oscaddr, makeBus|
		var type = myType ?? \button;
		addr = oscaddr ?? '/test';
		makeBus = makeBus ?? true;
		server = Server.default;
		
		bus = nil;
		val = nil;
		action = { };
		hasBus = false;
		
		if(makeBus, {
			hasBus = true;
			server.waitForBoot({
				bus = Bus.control(server, 1);
				server.sync;
				if(replAddr.class == NetAddr, {
/*					tracer = this.makeTraceRoutine;*/
					this.makeTraceStuff;
				});
			});
		});
		
		responder = this.makeOscResponder();
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
		bus.setSynchronous(val);
		if(runAction, {
			action.value();
		});
		if(replAddr.class == NetAddr, {
			replAddr.sendMsg(addr, val);
		});
	}
	
	makeOscResponder { 
		var f = OSCFunc({ |msg|
			if(hasBus, {
				bus.setSynchronous(msg[1]);
			});
			val = msg[1];
		}, addr, recvAddr);
		^f;
	}
	makeTraceRoutine {
		var r = Routine.run({
			var lastval, val;
			var i = 0;
			inf.do({
				i = i + 1;
				val = bus.getSynchronous;
				if((val!=lastval) || (i > 10), {
					i = 0;
					replAddr.sendMsg(addr, val);
				});
				(1/25).wait;
			});
		});
	}
	makeTraceStuff {
		traceNode = {
			var trig = Impulse.kr(25);
			SendReply.kr(trig, "/busvalue"++addr, In.kr(bus));
		}.play;
		traceResp = OSCFunc({ |msg|
			replAddr.sendMsg(addr, msg[3]);
		}, '/busvalue'++addr, recvAddr);
	}
}