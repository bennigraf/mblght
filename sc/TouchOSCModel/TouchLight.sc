/////// control main and background lighting, 5 each, 1 simple interface


TouchLight {
	
	classvar thisaddr, remoteaddr; // some NetAddrs...
	classvar <>patcher; // the patcher to talk to!
	
	classvar lights; // holds a couple of default lighting synths
	classvar >lfos;
	classvar >colors;
	
	var server;
	var controls;
	var <mainlights; // list with light objects in it (maybe make extra class from this?)
	var <backlights; // list with light objects in it
	var activeMainChan;
	var activeBackChan;
	var state; // holds state of preset - params, lfos, colors
	var strobo, fog;
	
	*initClass {
		lights = (main: this.mainLightSynths, back: this.backLightSynths());
		lfos = List();
		colors = List();
	}
	*mainLightSynths {
		// each synth should use: arg amp = 0, switch = 0; \param1.kr(0), \param2.kr(0), \color1.kr(0), \color2.kr(0)
		// main also has: \position.kr([x, y, width, lock]); // let's see if this array stuff works here...
		var l = List();
		l.add({
			// for now just polling some stuff...
			arg amp = 0, switch = 0;
/*			amp.poll(2, "amp");*/
/*			switch.poll(2, "switch");*/
			\param1.kr(0).poll(2, "param1");
			\param2.kr(0).poll(2, "param2");
/*			\color1.kr([0, 0, 0]).poll(2);*/
/*			\color2.kr([0, 0, 0]).poll(2);*/
/*			\position.kr([0, 0, 0, 0]).poll(2);*/
			1 * amp * switch;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		^l;
	}
	*backLightSynths {
		var l = List();
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		l.add({
			arg amp = 0, switch = 0;
			0;
		});
		^l;
	}
	
	*new { 
		^super.new.init();
	}
	
	init {
/*		if(thisaddr.isNil, {
			"Set a local address first".error;
			^false;
		});*/
/*		if(remoteaddr.isNil, {
			"Set a remote address first".error;
			^false;
		});*/
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
		
		state = this.emptyState;
		
		mainlights = List();
		backlights = List();
		activeMainChan = 0;
		activeBackChan = 0;
		strobo = ();
		fog = ();
		
		Routine.run({
			"hip".postln;
			this.initLights();
			"hap".postln;
			this.initControls();
			
		});
		
	}
	
	emptyState {
		var e = ();
		e[\main] = 5.collect({|n|
			var el = ();
			el[\param1] = (val: 0, lfo: (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0), lfoam: 0);
			el[\param2] = (val: 0, lfo: (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0), lfoam: 0);
			el[\color1] = (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0);
			el[\color2] = (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0);
			el;
		});
		e[\back] = 5.collect({|n|
			var el = ();
			el[\param1] = (val: 0, lfo: (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0), lfoam: 0);
			el[\param2] = (val: 0, lfo: (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0), lfoam: 0);
			el[\color1] = (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0);
			el[\color2] = (0: 0, 1: 0, 2:0, 3:0, 4:0, 5:0);
			el;
		});
	}
	
	// creates Controls
	initControls {
		controls = ();
		// global/shared controls
		// main chan selector
		controls[\mainchanselected] = List();
		5.do({ |n|
			var ctrl = TouchControl(\led, '/main/chan'++(n+1)++'sel', false);
			controls[\mainchanselected].add(ctrl);
		});
		controls[\mainchan] = TouchControl(\toggle, '/main/selectchan', false)
			.action_({|val| 
				if(val != 0, {
					activeMainChan = val;
					this.loadChan(\main, val);
					controls[\mainchanselected].do({|c, n| 
						if(n != (val-1), { c.set(0) }, {c.set(1) }); });
				});
			});
		
		// back chan selector
		controls[\backchanselected] = List();
		5.do({ |n|
			var ctrl = TouchControl(\led, '/back/chan'++(n+1)++'sel', false);
			controls[\backchanselected].add(ctrl);
		});
		controls[\backchan] = TouchControl(\toggle, '/back/selectchan', false)
			.action_({|val| 
				if(val != 0, {
					activeBackChan = val;
					controls[\backchanselected].do({|c, n| 
						if(n != (val-1), { c.set(0) }, {c.set(1) }); });
				});
			});
		
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
			controls[\back].add(aStrip);
		});
		
		// lfos and colors for main
		controls[\mainparams] = List();
		controls[\maincolors] = List();
		controls[\backparams] = List();
		controls[\backcolors] = List();
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
				var path;
				aParam[\fader] = TouchControl(\fader, '/'++where++'/param'++(n+1), false)
					.action_({|val|
						lights[activeChan][\paramsynths][n].set(\param, val);
						states[where][activeChan][\param++n][\val] = val;
					});
				aParam[\lfoam] = TouchControl(\fader, '/'++where++'/lfoam'++(n+1), false)
					.action_({|val|
						lights[activeChan][\paramsynths][n].set(\lfoam, val);
						states[where][activeChan][\param++n][\lfoam] = val;
					});
				6.do({ |o|
					aParam[(\lfo++(n+1)).asSymbol] = TouchControl(\toggle, '/'++where++'/param'++(n+1)++'lfo'++(o+1), false)
						.action_({ |val|
							lights[activeChan][\paramsynths][n].set(\lfo++(o+1), val);
							states[where][activeChan][\param++n][\lfo][o] = val;
						});
				});
				controls[where++'params'].add(aParam);
			});
			controls[where++'colors'] = List();
			2.do({ |n|
				var aClr = List();
				6.do({ |o|
					aClr.add(TouchControl(\toggle, '/'++where++'/color'++(n+1)++(o+1), false)
						.action_({ |val|
							lights[activeChan][\colorsynths][n].set(\clr++(o+1), val);
							states[where][activeChan][\color++n][o] = val;
						}) );
				});
				controls[where++'colors'].add(aClr);
			});
		});
		
		// position for main
		controls[\position] = TouchControl(\xy, '/main/pos', false)
			.action_({ |val|
				mainlights[activeMainChan][\positionsynth].set(\posx, val[0]);
				mainlights[activeMainChan][\positionsynth].set(\posy, val[1]);
			});
		controls[\positionwidth] = TouchControl(\fader, '/main/poswidth', false)
			.action_({ |val|
				mainlights[activeMainChan][\positionsynth].set(\poswidth, val);
			});
		controls[\positionlock] = TouchControl(\fader, '/main/poslock', false)
			.action_({ |val|
				mainlights[activeMainChan][\positionsynth].set(\poslock, val);
			});
		
		// strobo
		controls[\strobo] = ();
		controls[\strobo][\button] = TouchControl(\button, '/strobo', false)
			.action_({|val| strobo[\synth].set(\toggl, val); });
		controls[\strobo][\stroberate] = TouchControl(\toggle, '/strobo')
			.action_({|val|
				strobo[\synth].set(\stroberate, val * 0.9 + 0.1)
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
	
	loadChan { |where, val|
/*		controls[where][]*/
	}
	
	// creates actual lighting sdefs
	initLights {
		// for each light, load a nodechain?? or manually create one that actually gets all the
		// inputs available (on/off, amp, 2x param+lfo+am, color1, color2, for main: pos+lock+width)
		
		5.do({ |n|
			"hep".postln;
			mainlights.add(this.initLight(\main, n));
		});
		
		5.do({ |n|
			backlights.add(this.initLight(\back, n));
		});
		strobo = this.initLight(\strobo);
		server.sync;
		"neuffen".postln;
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
				server.sync;
/*				syn.set(\lfobus1, lfos[0].bus.index, \lfobus2, lfos[1].bus.index, \lfobus3, lfos[2].bus.index,
						\lfobus4, lfos[3].bus.index, \lfobus5, lfos[4].bus.index, \lfobus6, lfos[5].bus.index);*/
				syn.set(\lfobus1, lfos[0].bus.index, \lfobus2, lfos[1].bus.index, \lfobus3, lfos[2].bus.index,
						\lfobus4, lfos[3].bus.index, \lfobus5, lfos[4].bus.index, \lfobus6, lfos[5].bus.index);
				light[\paramsynths].add(syn);
			});
			
			// color synths
			light[\colorsynths] = List();
			2.do({
				var syn = this.aColorSynth;
				server.sync;
				syn.set(\clrbus1, colors[0].bus.index, \clrbus2, colors[1].bus.index, \clrbus3, colors[2].bus.index,
						\clrbus4, colors[3].bus.index, \clrbus5, colors[4].bus.index, \clrbus6, colors[5].bus.index);
				light[\colorsynths].add(syn);
			});
			server.sync;
			
			// actual light synths
			("wanna make the synth"+type+n).postln;
			light[\synth] = this.theSynth(type, n);
			server.sync;
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
		("making the synth"+type+n).postln;
		if(type == \main, {
			source = TouchLight.mainLightSynths.at(n);
		});
		if(type == \back, {
			source = TouchLight.backLightSynths.at(n);
		});
		"hoppel".postln;
		syn = NodeProxy.control(server).source_(source);
		"poppel".postln;
		server.sync;
		^syn;
	}
	
	aParamSynth {
		var synth = NodeProxy.control(server).source_({
			var param = \param.kr(0);
			var lfos = [\lfo1.kr(0), \lfo2.kr(0), \lfo3.kr(0), \lfo4.kr(0), \lfo5.kr(0), \lfo6.kr(0)];
			var lfobs = [In.kr(\lfobus1.kr(0)), In.kr(\lfobus2.kr(0)), In.kr(\lfobus3.kr(0)), In.kr(\lfobus4.kr(0)), In.kr(\lfobus5.kr(0)), In.kr(\lfobus6.kr(0))];
			var lfoam = \lfoam.kr(0);
			var lfosum = Mix.kr({ |n|
				Select.kr(lfos[n], [0, lfobs[n]]);
			}!6).clip(0, 1);
/*			param = param + (lfosum * 2 - 1 * lfoam);*/
			param = (param*2-1 * (1-lfoam)) + (lfosum*2-1 * lfoam) / 2 + 0.5;
			param;
		});
		server.sync;
		^synth;
	}
	aColorSynth {
		var synth;
		synth = NodeProxy.control(server, 3).source_({
			var clrs = [\clr1.kr(0), \clr2.kr(0), \clr3.kr(0), \clr4.kr(0), \clr5.kr(0), \clr6.kr(0)];
			var clrbs = [In.kr(\clrbus1.kr(0), 3), In.kr(\clrbus2.kr(0), 3), In.kr(\clrbus3.kr(0), 3),
						In.kr(\clrbus4.kr(0), 3), In.kr(\clrbus5.kr(0), 3), In.kr(\clrbus6.kr(0), 3)];
			var clrsum = Mix.kr({ |n|
				Select.kr(clrs[n], [0!3, clrbs[n]]);
			}!6).clip(0, 1);
			// TODO: only 5 clrs here???
			clrsum;
		});
		server.sync;
		^synth;
	}
	
	aPositionSynth {
		var synth = NodeProxy.control(server).source_({
			var position = [\posx.kr(0.5), \posy.kr(0.5), \poswidth.kr(0.5), \poslock.kr(0)];
/*			position.poll(1);*/
			position;
		});
		server.sync;
		^synth;
	}
	
	strobeSynth {
		var synth = NodeProxy.control(server).source_({
			var buses = Patcher.all.at(TouchLight.patcher).busesForGroupMethod(\strobo, \blitz);
			buses.do({
				Out.kr(buses, [\toggl.kr(0)/* * \intens.kr(0)*/, \stroberate.kr(0.1)]);
			});
			DC.kr(0);
		});
		^synth;
	}
	fogSynth {
		var synth = NodeProxy.control(server).source_({
			var buses = Patcher.all.at(TouchLight.patcher).busesForGroupMethod(\fog, \fog);
			var ph = Phasor.kr(\auto.kr(0), 1, -1, 60*ControlRate.ir);
			var fogsig = \toggl.kr(0) + (Trig1.kr(ph, 60 * \autointens.kr(0) * 0.8 + 0.2) * \auto.kr(0)).lag3(5);
			buses.do({
				Out.kr(buses, fogsig);
			});
			0;
		});
		^synth;
	}
}

