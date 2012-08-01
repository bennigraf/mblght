/////// control main and background lighting, 5 each, 1 simple interface


TouchLight {
	
	classvar thisaddr, remoteaddr; // some NetAddrs...
	classvar patcher; // the patcher to talk to!
	
	classvar lights; // holds a couple of default lighting synths
	classvar lfos;
	classvar colors;
	
	var server;
	var controls;
	var mainlights; // list with light objects in it (maybe make extra class from this?)
	var backlights; // list with light objects in it
	var activeMainChan;
	var activeBackChan;
	var strobo, fog;
	
	*initClass {
		lights = (main: this.mainLights, back: this.backLights());
		lfos = List();
		colors = List();
	}
	*mainLights {
		// each synth should use: arg amp = 0, switch = 0; \param1.kr(0), \param2.kr(0), \color1.kr(0), \color2.kr(0)
		// main also has: \position.kr([x, y, width, lock]); // let's see if this array stuff works here...
		var l = List();
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
	}
	*backLights {
		var l = List();
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
		l.add({
			// some synthie stuff
		});
	}
	
	*new { 
		^super.new.init();
	}
	
	init {
		if(thisaddr.isNil, {
			"Set a local address first".error;
			^false;
		});
		if(thisaddr.isNil, {
			"Set a remote address first".error;
			^false;
		});
		if(lfos.size<6, {
			"Pleeeeeaase add some lfos first!".error;
			^false;
		});
		if(colors.size<6, {
			"Would you be so kind to set some colors first?".error;
			^false;
		});
		if(patcher.isNil, {
			"Give me some patchin'!".error;
			^false;
		});
		
		server = Server.default;
		
		mainlights = List();
		backlights = List();
		strobo = ();
		fog = ();
		
		Routine.run({
			this.initLights();
			this.initControls();
			
		});
		
	}
	
	// creates Controls
	initControls {
		controls = ();
		// global/shared controls
		// main chan selector
		controls[\mainchan] = TouchControl(\fader, '/main/selectchan', false)
			.action_({|val| activeMainChan = val });
		
		// back chan selector
		controls[\backchan] = TouchControl(\fader, '/back/selectchan', false)
			.action_({|val| activeBackChan = val });
		
		// main chan faders
		controls[\main] = List();
		5.do({ |n|
			var aStrip = ();
			aStrip[\fader] = TouchControl(\fader, '/main/chan'++(n+1)++'fader')
				.action_({ |val|
					mainlights[n][\synth].set(\amp, val);
				});
			aStrip[\toggle] = TouchControl(\toggle, '/main/chan'++(n+1)++'toggle')
				.action_({ |val|
					mainlights[n][\synth].set(\switch, val);
				});
			controls[\main].add(aStrip);
		});
		
		// back chan faders
		controls[\back] = List();
		5.do({ |n|
			var aStrip = ();
			aStrip[\fader] = TouchControl(\fader, '/back/chan'++(n+1)++'fader');
			aStrip[\toggle] = TouchControl(\toggle, '/back/chan'++(n+1)++'toggle');
			controls[\main].add(aStrip);
		});
		
		// lfos and colors for main
		controls[\mainparams] = List();
		controls[\maincolors] = List();
		['main', 'back'].do({ |where|
			var activeChan, lights;
			if(where == 'main', {
				lights = mainlights;
				activeChan = activeMainChan;
			}, {
				lights = backlights;
				activeChan = activeBackChan;
			});
			controls[where++'params'] = List();
			2.do({ |n|
				var aParam = ();
				aParam[\fader] = TouchControl(\fader, '/main/param'++(n+1), false)
					.action_({|val|
						lights[activeChan][\paramsynths][n].set(\param, val);
					});
				6.do({ |o|
					aParam[(\lfo++(n+1)).asSymbol] = TouchControl(\toggle, '/main/param'++(n+1)++'lfo'++(o+1), false)
						.action_({ |val|
							lights[activeChan][\paramsynths][n].set(\lfo++(o+1), val);
						});
				});
				aParam[\lfoam] = TouchControl(\fader, '/main/lfoam'++(n+1), false)
					.action_({|val|
						lights[activeChan][\paramsynths][n].set(\lfoam, val);
					});
				controls[where++'params'].add(aParam);
			});
			controls[where++'colors'] = List();
			2.do({ |n|
				var aClr = List();
				6.do({ |o|
					aClr.add(TouchControl(\toggle, '/main/color'++(n+1)++(o+1), false)
						.action_({ |val|
							lights[activeChan][\colorsynths][n].set(\clr++(o+1), val);
						}) );
				});
				controls[where++'colors'].add(aClr);
			});
		});
		
		// position for main
		controls[\position] = TouchControl(\xy, '/main/pos', false)
			.action_({ |val|
				mainlights[\positionsynth].set(\posx, val[0], \posy, val[1]);
			});
		controls[\positionwidth] = TouchControl(\fader, '/main/poswidth', false)
			.action_({ |val|
				mainlights[\positionsynth].set(\poswidth, val);
			});
		controls[\positionlock] = TouchControl(\fader, '/main/poslock', false)
			.action_({ |val|
				mainlights[\positionsynth].set(\poslock, val);
			});
		
		// strobo
		controls[\strobo] = ();
		controls[\strobo][\button] = TouchControl(\button, '/strobo', false)
			.action_({|val| strobo[\synth].set(\toggl, val); });
		controls[\strobo][\rate] = TouchControl(\toggle, '/strobo')
			.action_({|val|
				strobo[\synth].set(\rate, val * 0.9 + 0.1)
			});
		// fog
		controls[\fog] = ();
		controls[\fog][\button] = TouchControl(\button, '/fog', false)
			.action_({|val|	fog[\synth].set(\toggl, val) });
		controls[\fog][\button] = TouchControl(\button, '/autofog', false)
			.action_({|val|	fog[\synth].set(\auto, val) });		
		controls[\fog][\button] = TouchControl(\button, '/autofog/intens', false)
			.action_({|val|	fog[\synth].set(\autointens, 1) });
		
	}
	
	// creates actual lighting sdefs
	initLights {
		// for each light, load a nodechain?? or manually create one that actually gets all the
		// inputs available (on/off, amp, 2x param+lfo+am, color1, color2, for main: pos+lock+width)
		
		5.do({ |n|
			mainlights.add(this.initLight(\main, n));
		});
		5.do({ |n|
			backlights.add(this.initLight(\back, n));
		});
		strobo = this.initLight(\strobo);
		fog = this.initLight(\fog);
		server.sync;
	}
	
	initLight { |type, n|
		var light = ();
		if(type == \main, {
			light[\positionsynth] = this.aPositionSynth;
		});
		if(type == \back, {
			
		});
		if((type == \main) || (type == \back), {
			// param synths
			light[\paramsynths] = List();
			2.do({
				var syn = this.aParamSynth;
				syn.set(\lfobus1, lfos[0].bus.index, \lfobus2, lfos[1].bus.index, \lfobus3, lfos[2].bus.index,
						\lfobus4, lfos[3].bus.index, \lfobus5, lfos[4].bus.index, \lfobus6, lfos[5].bus.index);
				light[\paramsynths].add(syn);
			});
			
			// color synths
			light[\colorsynths] = List();
			2.do({
				var syn = this.aColorSynth;
				syn.set(\clrbus1, colors[0].bus.index, \clrbus2, colors[1].bus.index, \clrbus3, colors[2].bus.index,
						\clrbus4, colors[3].bus.index, \clrbus5, colors[4].bus.index, \clrbus6, colors[5].bus.index);
				light[\colorsynths].add(syn);
			});
			server.sync;
			
			// actual light synths
			light[\synth] = this.theSynth(type, n);
			light[\synth].map(\param1, light[\paramsynths][0]);
			light[\synth].map(\param2, light[\paramsynths][1]);
			light[\synth].map(\color1, light[\colorsynths][0]);
			light[\synth].map(\color2, light[\colorsynths][1]);
		});
		if(type == \main, {
			light[\synth].map(\position, light[\positionsynth]);
		});
		if(type == \strobo, {
			var light = ();
			light[\synth] = this.strobeSynth;
		});
		if(type == \fog, {
			// fog... auto mode!!
			var light = ();
			light[\synth] = this.fogSynth;
		});
		server.sync;
		^light;
	}
	
	// make the actual light playing synth
	theSynth { |type, n|
		var syn, source;
		if(type == \main, {
			source = TouchLight.mainLights[n];
		});
		if(type == \back, {
			source = TouchLight.backLights[n];
		});
		syn = NodeProxy.control(server).source_(source);
		^syn;
	}
	
	aParamSynth {
		var synth = NodeProxy.control(server).source_({
			arg lfobus1 = 0, lfobus2 = 0, lfobus3 = 0, lfobus4 = 0, lfobus5 = 0, lfobus6 = 0;
			var param = \param.kr(0);
			var lfos = [\lfo1.kr(0), \lfo2.kr(0), \lfo3.kr(0), \lfo4.kr(0), \lfo5.kr(0), \lfo6.kr(0)];
			var lfobs = [In.kr(lfobus1), In.kr(lfobus2), In.kr(lfobus3), In.kr(lfobus4), In.kr(lfobus5), In.kr(lfobus6)];
			var lfoam = \lfoam.kr(0);
			var lfosum = Mix.kr([Lfosw.kr(lfos[0], lfobs[0]), Lfosw.kr(lfos[1], lfobs[1]), Lfosw.kr(lfos[2], lfobs[2]),
								Lfosw.kr(lfos[3], lfobs[3]), Lfosw.kr(lfos[4], lfobs[4]), Lfosw.kr(lfos[5], lfobs[5])]).clip(0, 1);
			param = param + (lfosum * 2 - 1 * lfoam);
			param;
		});
		^synth;
	}
	aColorSynth {
		var synth = NodeProxy.control(server, 3).source_({
			arg clrbus1 = 0, clrbus2 = 0, clrbus3 = 0, clrbus4 = 0, clrbus5 = 0, clrbus6 = 0;
			var clrs = [\clr1.kr(0), \clr2.kr(0), \clr3.kr(0), \clr5.kr(0), \clr6.kr(0)];
			var clrbs = [In.kr(clrbus1, 3), In.kr(clrbus2, 3), In.kr(clrbus3, 3),
						In.kr(clrbus4, 3), In.kr(clrbus5, 3), In.kr(clrbus6, 3)];
			var clrsum = Mix.kr({ |n|
				Select.kr(clrs[n], [0!3, clrbs[n]]);
			}!6).clip(0, 1);
		});
		^synth;
	}
	
	aPositionSynth {
		var synth = NodeProxy.control(server, 2).source_({
			var position = [\posx.kr(0.5), \posy.kr(0.5), \poswidth.kr(0.5), \poslock.kr(0)];
			position;
		});
		^synth;
	}
	
	strobeSynth {
		var synth = NodeProxy.control(server).source_({
			var buses = Patcher.all.at(patcher).busesForGroupMethod(\strobo, \blitz);
			buses.do({
				Out.kr(buses, [\toggl.kr(0)/* * \intens.kr(0)*/, \rate.kr(0.1)]);
			});
		});
		^synth;
	}
	fogSynth {
		var synth = NodeProxy.control(server).source_({
			var buses = Patcher.all.at(patcher).busesForGroupMethod(\fog, \fog);
			var ph = Phasor.kr(\auto.kr(0), 1, -1, 60*ControlRate.ir);
			var fogsig = \toggl.kr(0) + (Trig1.kr(ph, 60 * \autointens.kr(0) * 0.8 + 0.2) * \auto.kr(0)).lag3(5);
			buses.do({
				Out.kr(buses, fogsig);
			});
		});
		^synth;
	}
}

