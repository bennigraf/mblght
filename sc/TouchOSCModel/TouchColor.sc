TouchColor {
	var server;
	var colorNode;
	var controls;
	var id;
	var colorNode;
	var mode; // \hsv or \rgb
	
	*initClass {
		
	}
	
	*new { |myId|
		^super.new.init(myId);
	}
	
	init { |myId|
		id = myId;
		server = Server.default;
		mode = \hsv;
		
		this.initControls();
		this.colorNode();
		colorNode.set(\mode, 0);
		colorNode.set(\hbus, controls.h.bus);
		colorNode.set(\sbus, controls.s.bus);
		colorNode.set(\vbus, controls.v.bus);
		colorNode.set(\rbus, controls.r.bus);
		colorNode.set(\gbus, controls.g.bus);
		colorNode.set(\bbus, controls.b.bus);
		this.initButtons();
	}
	
	initControls {
		controls = ();
		controls.r = TouchControl(\fader, '/color'++id++'/r');
		controls.g = TouchControl(\fader, '/color'++id++'/g');
		controls.b = TouchControl(\fader, '/color'++id++'/b');
		controls.rlfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/r/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 'r', val) });
			controls.rlfo.add(acontrol);
		});
		controls.glfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/g/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 'g', val) });
			controls.glfo.add(acontrol);
		});
		controls.blfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/b/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 'b', val) });
			controls.blfo.add(acontrol);
		});
		
		controls.h = TouchControl(\fader, '/color'++id++'/h');
		controls.s = TouchControl(\fader, '/color'++id++'/s');
		controls.v = TouchControl(\fader, '/color'++id++'/v');
		controls.hlfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/h/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 'g', val) });
			controls.hlfo.add(acontrol);
		});
		controls.slfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/s/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 's', val) });
			controls.slfo.add(acontrol);
		});
		controls.vlfo = List();
		6.do({ |n|
			var acontrol = TouchControl(\toggle, '/color'++id++'/v/lfo'++(n+1), \false)
				.action_({|val| this.maplfo(n+1, 'v', val) });
			controls.vlfo.add(acontrol);
		});
	}
	
	maplfo { |n, clr, val|
		if(val == 1, {
			if(lfo[n].bus.class == Bus, {
				colorNode.map(\lfo++clr++n, lfo[n].bus);
			});
		}, {
			colorNode.map(\lfo++clr++n, nil);
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
		controls[clr++'lfo'].do({|ctrl|
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
			if(myMode == \hsv, {
/*				mode = \hsv*/
			});
		});
	}
	
	colorNode {
		colorNode = NodeProxy.control(server, 3).source_({ 
			arg mode = 0, // mode 0 is hsv, 1 is rgb
				rbus = 0, // buses where values of params come from (touchcontrol)
				gbus = 0,
				bbus = 0,
				hbus = 0,
				sbus = 0,
				vbus = 0,
				lfor = 0, lfog = 0, lfob = 0, lfoh = 0, lfos = 0, lfov = 0, // global lfo switches
				lfor1 = 0, lfor2 = 0, lfor3 = 0, lfor4 = 0, lfor5 = 0, lfor6 = 0,
				lfog1 = 0, lfog2 = 0, lfog3 = 0, lfog4 = 0, lfog5 = 0, lfog6 = 0,
				lfob1 = 0, lfob2 = 0, lfob3 = 0, lfob4 = 0, lfob5 = 0, lfob6 = 0,
				lfoh1 = 0, lfoh2 = 0, lfoh3 = 0, lfoh4 = 0, lfoh5 = 0, lfoh6 = 0,
				lfos1 = 0, lfos2 = 0, lfos3 = 0, lfos4 = 0, lfos5 = 0, lfos6 = 0,
				lfov1 = 0, lfov2 = 0, lfov3 = 0, lfov4 = 0, lfov5 = 0, lfov6 = 0;
				
			var clr;
			
			var rgb = [In.kr(rbus), In.kr(gbus), In.kr(bbus)];
			var hsv = [In.kr(hbus), In.kr(sbus), In.kr(vbus)];
			
			var lforsum = Mix.kr(\lfor1.kr(0), \lfor2.kr(0), \lfor3.kr(0), \lfor4.kr(0), \lfor5.kr(0), \lfor6);
			var lfogsum = Mix.kr(\lfog1.kr(0), \lfog2.kr(0), \lfog3.kr(0), \lfog4.kr(0), \lfog5.kr(0), \lfog6);
			var lfobsum = Mix.kr(\lfob1.kr(0), \lfob2.kr(0), \lfob3.kr(0), \lfob4.kr(0), \lfob5.kr(0), \lfob6);
			var lfohsum = Mix.kr(\lfoh1.kr(0), \lfoh2.kr(0), \lfoh3.kr(0), \lfoh4.kr(0), \lfoh5.kr(0), \lfoh6);
			var lfossum = Mix.kr(\lfos1.kr(0), \lfos2.kr(0), \lfos3.kr(0), \lfos4.kr(0), \lfos5.kr(0), \lfos6);
			var lfovsum = Mix.kr(\lfov1.kr(0), \lfov2.kr(0), \lfov3.kr(0), \lfov4.kr(0), \lfov5.kr(0), \lfov6);
							
			hsv = [Select.kr(lfoh, [hbus, lforsum]), Select.kr(lfos, [sbus, lfossum]), Select.kr(lfov, [vbus, lfovsum])];
			rgb = [Select.kr(lfor, [rbus, lforsum]), Select.kr(lfob, [bbus, lfobsum]), Select.kr(lfob, [bbus, lfobsum])];
			
			clr = Select.kr(mode, [Hsv2rgb.kr(hsv[0], hsv[1], hsv[2]), rgb]);
			// play out to TouchControl
/*			Out.kr(controls.phase.bus, phas);*/
			// play out to NodeProxy-Bus
			clr;
		});
	}
	
}

Lfosw : UGen {
	*kr { |which, bus|
		var select = Select.kr(which, [0, In.kr(bus)]);
	}
}