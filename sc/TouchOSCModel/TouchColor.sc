TouchColor {
	
	classvar <>lfos; // holds TouchLFOs
	
	var server;
	var <colorNode;
	var <controls;
	var id;
	var colorNode;
	var mode; // \hsv or \rgb
	
	var traceResponders;
	
	*initClass {
		lfos = List();
	}
	
	*new { |myId|
		^super.new.init(myId);
	}
	
	*addLfo { |lfo|
		lfos.add(lfo);
	}
	*removeLfoAt { |index|
		lfos.removeAt(index);
	}
	
	init { |myId|
		id = myId;
		server = Server.default;
		mode = \hsv;
		Routine.run({
			this.initControls();
			this.makeColorNode();
			this.makeTraceResponders();
			server.sync; // server sync must happen inside subroutines?!
			colorNode.set(\mode, 0); // hsv
			colorNode.set(\hbus, controls.h.bus.index);
			colorNode.set(\sbus, controls.s.bus.index);
			colorNode.set(\vbus, controls.v.bus.index);
			colorNode.set(\rbus, controls.r.bus.index);
			colorNode.set(\gbus, controls.g.bus.index);
			colorNode.set(\bbus, controls.b.bus.index);
			lfos.do({ |lfo, n|
				var bus = 'lfo'++(n+1)++'bus';
				lfo.postln;
				lfo.bus.postln;
				lfo.bus.index.postln;
				colorNode.set(bus, lfo.bus.index);
			});
		});
	}
	
	initControls {
		controls = ();
		controls.r = TouchControl(\fader, '/color'++id++'/r')
			.action_({ |val| this.setMode(\rgb); });
		controls.g = TouchControl(\fader, '/color'++id++'/g')
			.action_({ |val| this.setMode(\rgb); });
		controls.b = TouchControl(\fader, '/color'++id++'/b')
			.action_({ |val| this.setMode(\rgb); });
		controls.rlfo = List();
		lfos.size.do({ |n|
			var address = '/color'++id++'/r/lfo'++(n+1);
			var acontrol = TouchControl(\toggle, address.asSymbol, false)
				.action_({|val| this.maplfo(n+1, 'r', val) });
			controls.rlfo.add(acontrol);
		});
		controls.glfo = List();
		lfos.size.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/g/lfo'++(n+1), false)
				.action_({|val| this.maplfo(n+1, 'g', val) });
			controls.glfo.add(acontrol);
		});
		controls.blfo = List();
		lfos.size.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/b/lfo'++(n+1), false)
				.action_({|val| this.maplfo(n+1, 'b', val) });
			controls.blfo.add(acontrol);
		});
		
		controls.h = TouchControl(\fader, '/color'++id++'/h')
			.action_({ |val| this.setMode(\hsv); });
		controls.s = TouchControl(\fader, '/color'++id++'/s')
			.action_({ |val| this.setMode(\hsv); });
		controls.v = TouchControl(\fader, '/color'++id++'/v')
			.action_({ |val| this.setMode(\hsv); });
		controls.hlfo = List();
		lfos.size.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/h/lfo'++(n+1), false)
				.action_({|val| this.maplfo(n+1, 'h', val); ("h - lfo"+n).postln });
			controls.hlfo.add(acontrol);
		});
		controls.slfo = List();
		lfos.size.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/s/lfo'++(n+1), false)
				.action_({|val| this.maplfo(n+1, 's', val) });
			controls.slfo.add(acontrol);
		});
		controls.vlfo = List();
		lfos.size.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/v/lfo'++(n+1), false)
				.action_({|val| this.maplfo(n+1, 'v', val) });
			controls.vlfo.add(acontrol);
		});
		
		server.sync;
	}
	
	maplfo { |n, clr, val|
		"mapping".postln;
		[n ,clr, val].postln;
		if(val == 1, {
			if(lfos[n-1].bus.class == Bus, {
/*				colorNode.map(\lfo++clr++n, lfos[n-1].bus);*/
				colorNode.set(\lfo++clr++n, 1);
			});
		}, {
/*			colorNode.map(\lfo++clr++n, nil);*/
			colorNode.set(\lfo++clr++n, 0);
		});
		this.checkLfoState(clr);
		if((clr == 'h') || (clr == 's') || (clr == 'v'), {
			this.setMode(\hsv);
		});
		if((clr == 'r') || (clr == 'g') || (clr == 'b'), {
			this.setMode(\rgb);
		});
	}
	
	checkLfoState { |clr|
		var state = \fader;
		controls[(clr++'lfo').asSymbol].do({|ctrl, n|
			if(ctrl.val == 1, {
				state = \lfo;
			});
		});
		if(state == \fader, {
			colorNode.set(\lfo++clr, 0);
		}, {
			colorNode.set(\lfo++clr, 1);
		});
	}
	
	setMode { |myMode|
		if(myMode != mode, {
			mode = myMode;
			if(mode == \hsv, {
				colorNode.set(\mode, 0);
			}, {
				colorNode.set(\mode, 1);
			});
		});
	}
	
	makeColorNode {
		colorNode = NodeProxy.control(server, 3).source_({ 
			arg mode = 0, // mode 0 is hsv, 1 is rgb
				rbus = 0, // buses where values of params come from (touchcontrol)
				gbus = 0,
				bbus = 0,
				hbus = 0,
				sbus = 0,
				vbus = 0,
				lfoh = 0, lfos = 0, lfov = 0, lfor = 0, lfog = 0, lfob = 0, // global lfo switches
				lfo1bus = 0, lfo2bus = 0, lfo3bus = 0, lfo4bus = 0, lfo5bus = 0, lfo6bus = 0, // global lfo buses
				lfor1 = 0, lfor2 = 0, lfor3 = 0, lfor4 = 0, lfor5 = 0, lfor6 = 0,
				lfog1 = 0, lfog2 = 0, lfog3 = 0, lfog4 = 0, lfog5 = 0, lfog6 = 0,
				lfob1 = 0, lfob2 = 0, lfob3 = 0, lfob4 = 0, lfob5 = 0, lfob6 = 0,
				lfoh1 = 0, lfoh2 = 0, lfoh3 = 0, lfoh4 = 0, lfoh5 = 0, lfoh6 = 0,
				lfos1 = 0, lfos2 = 0, lfos3 = 0, lfos4 = 0, lfos5 = 0, lfos6 = 0,
				lfov1 = 0, lfov2 = 0, lfov3 = 0, lfov4 = 0, lfov5 = 0, lfov6 = 0;
/*				lfor1bus = 0, lfor2bus = 0, lfor3bus = 0, lfor4bus = 0, lfor5bus = 0, lfor6bus = 0,
				lfog1bus = 0, lfog2bus = 0, lfog3bus = 0, lfog4bus = 0, lfog5bus = 0, lfog6bus = 0,
				lfob1bus = 0, lfob2bus = 0, lfob3bus = 0, lfob4bus = 0, lfob5bus = 0, lfob6bus = 0,
				lfoh1bus = 0, lfoh2bus = 0, lfoh3bus = 0, lfoh4bus = 0, lfoh5bus = 0, lfoh6bus = 0,
				lfos1bus = 0, lfos2bus = 0, lfos3bus = 0, lfos4bus = 0, lfos5bus = 0, lfos6bus = 0,
				lfov1bus = 0, lfov2bus = 0, lfov3bus = 0, lfov4bus = 0, lfov5bus = 0, lfov6bus = 0;*/
				
			var clr;
			var trig = Impulse.kr(25); // for tracing
			
			var rgb = [In.kr(rbus), In.kr(gbus), In.kr(bbus)];
			var hsv = [In.kr(hbus), In.kr(sbus), In.kr(vbus)];
			
/*			var lforsum = Mix.kr(\lfor1.kr(0), \lfor2.kr(0), \lfor3.kr(0), \lfor4.kr(0), \lfor5.kr(0), \lfor6);
			var lfogsum = Mix.kr(\lfog1.kr(0), \lfog2.kr(0), \lfog3.kr(0), \lfog4.kr(0), \lfog5.kr(0), \lfog6);
			var lfobsum = Mix.kr(\lfob1.kr(0), \lfob2.kr(0), \lfob3.kr(0), \lfob4.kr(0), \lfob5.kr(0), \lfob6);
			var lfohsum = Mix.kr(\lfoh1.kr(0), \lfoh2.kr(0), \lfoh3.kr(0), \lfoh4.kr(0), \lfoh5.kr(0), \lfoh6);
			var lfossum = Mix.kr(\lfos1.kr(0), \lfos2.kr(0), \lfos3.kr(0), \lfos4.kr(0), \lfos5.kr(0), \lfos6);
			var lfovsum = Mix.kr(\lfov1.kr(0), \lfov2.kr(0), \lfov3.kr(0), \lfov4.kr(0), \lfov5.kr(0), \lfov6);*/
			
			var lforsum = Mix.kr([Lfosw.kr(lfor1, lfo1bus), Lfosw.kr(lfor2, lfo2bus), Lfosw.kr(lfor3, lfo3bus), 
								 Lfosw.kr(lfor4, lfo4bus), Lfosw.kr(lfor5, lfo5bus), Lfosw.kr(lfor6, lfo6bus)]).clip(0, 1);
			var lfogsum = Mix.kr([Lfosw.kr(lfog1, lfo1bus), Lfosw.kr(lfog2, lfo2bus), Lfosw.kr(lfog3, lfo3bus), 
								 Lfosw.kr(lfog4, lfo4bus), Lfosw.kr(lfog5, lfo5bus), Lfosw.kr(lfog6, lfo6bus)]).clip(0, 1);
			var lfobsum = Mix.kr([Lfosw.kr(lfob1, lfo1bus), Lfosw.kr(lfob2, lfo2bus), Lfosw.kr(lfob3, lfo3bus), 
								 Lfosw.kr(lfob4, lfo4bus), Lfosw.kr(lfob5, lfo5bus), Lfosw.kr(lfob6, lfo6bus)]).clip(0, 1);
			var lfohsum = Mix.kr([Lfosw.kr(lfoh1, lfo1bus), Lfosw.kr(lfoh2, lfo2bus), Lfosw.kr(lfoh3, lfo3bus), 
								 Lfosw.kr(lfoh4, lfo4bus), Lfosw.kr(lfoh5, lfo5bus), Lfosw.kr(lfoh6, lfo6bus)]).clip(0, 1);
			var lfossum = Mix.kr([Lfosw.kr(lfos1, lfo1bus), Lfosw.kr(lfos2, lfo2bus), Lfosw.kr(lfos3, lfo3bus), 
								 Lfosw.kr(lfos4, lfo4bus), Lfosw.kr(lfos5, lfo5bus), Lfosw.kr(lfos6, lfo6bus)]).clip(0, 1);
			var lfovsum = Mix.kr([Lfosw.kr(lfov1, lfo1bus), Lfosw.kr(lfov2, lfo2bus), Lfosw.kr(lfov3, lfo3bus), 
								 Lfosw.kr(lfov4, lfo4bus), Lfosw.kr(lfov5, lfo5bus), Lfosw.kr(lfov6, lfo6bus)]).clip(0, 1);

			[lfor1, lfor2, lfor3, lfor4, lfor5, lfor6].poll(1);
			[lforsum].poll(1);
						
			hsv = [Select.kr(lfoh, [hsv[0], lforsum]), Select.kr(lfos, [hsv[1], lfossum]), Select.kr(lfov, [hsv[2], lfovsum])];
			rgb = [Select.kr(lfor, [rgb[0], lforsum]), Select.kr(lfob, [rgb[1], lfobsum]), Select.kr(lfob, [rgb[2], lfobsum])];
			clr = Select.kr(mode, [Hsv2rgb.kr(hsv[0], hsv[1], hsv[2]), rgb]);
			
			// trace stuff
			SendReply.kr(trig * lfoh, "/color"++id++"/lfohsum", lfohsum);
			SendReply.kr(trig * lfos, "/color"++id++"/lfossum", lfossum);
			SendReply.kr(trig * lfov, "/color"++id++"/lfovsum", lfovsum);
			SendReply.kr(trig * lfor, "/color"++id++"/lforsum", lforsum);
			SendReply.kr(trig * lfog, "/color"++id++"/lfogsum", lfogsum);
			SendReply.kr(trig * lfob, "/color"++id++"/lfobsum", lfobsum);
			
			clr;
		});
		server.sync;
	}
	
	bus {
		^colorNode.bus;
	}
	
	makeTraceResponders {
		var clrs = ['h', 's', 'v', 'r', 'g', 'b'];
		traceResponders = List();
		clrs.do({|clr|
			traceResponders.add(OSCFunc({ |msg|
				var val = msg[3]; // this magic is .copyRange!
/*				TouchControl.replAddr.sendMsg('/color'++id++'/'++clr, val); // The star unpacks the array somehow*/
				controls[clr].set(msg[3], false); // no action...
			}, '/color'++id++'/lfo'++clr++'sum', TouchControl.recvAddr));
		});
	}
	
}

Lfosw : UGen {
	*kr { |which, bus|
		var select = Select.kr(which, [0, In.kr(bus)]);
		^select
	}
}