LGui {
	*new {
		^super.new.init();
	}
	
	init {
		LGui_Main();
	}
	
}




LGui_Main {
	
	var window;
	var server;
	var updateActions;
	
	*new {
		^super.new.init();
	}
	
	init {
		server = Server.default;
		updateActions = List();
		window = Window.new("Lighting Controller", Rect(400, 400, 400, 400)).front;
		this.makeDefaultWindow();
		this.updateView();
	}
	
	makeDefaultWindow {
		window.layout_(
			VLayout(
				this.serverCheckView(),
				this.patcherView(),
				this.theSaviour();
				)
		);
	}
	
	serverCheckView {
		var view;
		var string = "Server:";
		var txt, btn, updateAction;
/*		var text = StaticText(view, 200@20).string_();*/
		
		if(server.pid.isNil, {
			string = string + "Not running!";
		}, {
			string = string + "Running...";
		});
		
		txt = StaticText(nil, 200@20).string_(string);
		
		btn = Button(nil, 80@20).states_([
			["Boot Server", Color.black, Color.white],
			["Stop Server", Color.black, Color.green] ])
		.action_({ |view|
			if(view.value == 1, {
				server.boot;
				this.updateView();
			}, {
				server.quit;
				this.updateView();
			});
		});
		updateAction = {
			var string;
			if(server.pid.isNil, {
				string = "Server: Not running!";
				btn.value_(0);
			}, {
				string = "Server: Running...";
				btn.value_(1);
			});
			txt.string_(string);
		};
		updateAction.value();
		updateActions.add(updateAction);
		
		view = HLayout(txt, btn);
		
		^view;
	}
	
	patcherView {
		var view;
		var hdln;
		var ptchrbx, btns, addr, rmvr;
		
		// headline...
		hdln = StaticText(nil, 200@20).string_("Patcher:")
			.font_(Font.sansSerif(18, true));
		
		// box that lists all the active patchers
		ptchrbx = ListView(nil, Rect(0, 0, 200, 200));
		updateActions.add({
			var ptchrs = Patcher.all;
			var ptchrarr = [];
			ptchrs.keysDo({ |ptchr|
				ptchrarr = ptchrarr.add(ptchr);
			});
			ptchrbx.items_(ptchrarr);
		});
		
		// buttons for edit/manage devices/remove of patcher
		rmvr = Button().states_([ ["Remove Patcher"] ])
			.action_({
				var patcherName = ptchrbx.items.at(ptchrbx.value);
				Patcher.all.at(patcherName).buffers.do({|buf|
					buf.close()
				});
				Patcher.all.at(patcherName).end();
/*				ptchrbx.items = ptchrbx.items.removeAt(ptchrbx.value);*/
				this.updateView();
			});
		btns = VLayout(
			HLayout(
				[Button().states_([ ["Setup"] ])
					.action_({LGui_SetupPatcher(ptchrbx.items.at(ptchrbx.value))}),
				 a:\top],
				[Button().states_([ ["Devices"] ])
					.action_({LGui_manageDevices(ptchrbx.items.at(ptchrbx.value))}),
				a:\top]
			),
			rmvr;
		);
		
		// button: add patcher...
		addr = Button().states_([ ["Add Patcher"] ])
			.action_({
				this.addPatcher;
			});
		
		view = VLayout(
			hdln, 
			HLayout(ptchrbx, btns),
			addr
		);
		
		^view;
	}
	
	addPatcher {
		var dialog;
		var bounds = Window.availableBounds;
		var addbtn, ptchrname;
		
		dialog = Window("Add Patcher", Rect(bounds.width/2-200, bounds.height/2+50, 400, 100));
		
		ptchrname = TextField();
		
		addbtn = Button().states_([["Create Patcher"]])
			.action_({ |btn|
				var patcher = Patcher.all();
				var check = true;
				if(ptchrname.value.isNil, {
					"No name given!".postln;
					check = false;
				});
				if(patcher[ptchrname.value.asSymbol].notNil, {
					"Patcher already exists!".postln;
					check = false;
				});
				if(check, {
					dialog.close;
					Patcher.new(ptchrname.value.asSymbol);
					this.updateView();
				});
			});
		
		dialog.layout_(VLayout(
			HLayout(
				StaticText().string_("Patcher name:"),
				ptchrname
			),
			[addbtn, a:\topleft]
		));
		
		
		dialog.front;
	}
	
	theSaviour {
		var view;
		var slctr, ldbtn, svbtn;
		
		slctr = PopUpMenu()
			.items_(["(default)", [""]]);
			
		ldbtn = Button().states_([["Load"]]);
		svbtn = Button().states_([["Save"]]);
		
		view = VLayout(
			StaticText().string_("Load/Save Setup").font_(Font.sansSerif(18, true)),
/*			StaticText(nil, 200@20).string_("Patcher:")
				.font_(Font.sansSerif(18, true));*/
			HLayout(
				slctr, ldbtn, svbtn
			)
		);
		
		^view;
	}
	
	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}
	
/*	Window.availableBounds*/

}


LGui_SetupPatcher {
	
	var window;
	var updateActions;
	var patcher;
	
	*new { |patcherid|
		^super.new.init(patcherid);
	}
	
	init { |patcherid|
		updateActions = List();
		patcher = Patcher.all.at(patcherid);
		this.setupPatcher();
		this.updateView();
	}
	
	setupPatcher { |patcherid|
		var bfrslst, devslst;
		var addBfrBtn, rmvBfrBtn;
		var devslctr, addDevBtn, rmvDevBtn;
		
		window = Window("Setup Patcher" + patcher.id);
		
		bfrslst = ListView()
			.action_({ this.updateView() });
		updateActions.add({
			var val = bfrslst.value;
			bfrslst.items = patcher.buffers.collect({ |bfr, n|
				"A Buffer";
			});
			if(val.notNil, {
				if(val < bfrslst.items.size, {
					bfrslst.value = val;
				});
			});
		});
		
		addBfrBtn = Button().states_([["Add Buffer"]])
			.action_({ 
				this.addBufferToPatcher(patcher);
				this.updateView();
			});
		rmvBfrBtn = Button().states_([["Remove Buffer"]])
			.action_({
				var buffer = bfrslst.items.at(bfrslst.value());
				if(buffer.notNil, {
					this.removeBufferFromPatcher(bfrslst.value, patcher);
					this.updateView();
				});
			});
		
		devslst = ListView();
		updateActions.add({
			if(bfrslst.value().notNil, {
				var buffer = patcher.buffers.at(bfrslst.value());
				var items = [];
				buffer.devices().array.do({ |dev|
					var str = dev.asString;
					if({dev.describe}.try.notNil, {
						str = str + "("++dev.describe++")";
					});
					items = items.add(str);
				});
				devslst.items = items;
			}, {
				devslst.items = [];
			});
		});

		devslctr = PopUpMenu()
			.items_(DmxBuffer.knownDevices.collect({|dev| dev.asString}));
		addDevBtn = Button().states_([["Add Device to Buffer"]])
			.action_({
				var devclass = devslctr.items.at(devslctr.value());
				this.addDeviceToBuffer(devclass, patcher.buffers.at(bfrslst.value()))
			});
		rmvDevBtn = Button().states_([["Remove Device from Buffer"]])
			.action_({
				var buffer = patcher.buffers.at(bfrslst.value());
				var devIndex = devslst.value();
				this.removeDeviceFromBuffer(devIndex, buffer);
			});
		
		window.layout = VLayout(
			StaticText().string_("Buffers:").font_(Font.sansSerif(18, true)),
			bfrslst,
			HLayout(addBfrBtn, rmvBfrBtn),
			StaticText().string_("Output Devices:").font_(Font.sansSerif(18, true)),
			devslst,
			[devslctr, a:\left],
			HLayout(addDevBtn, rmvDevBtn)
		);
		
		window.front;
	}
	addBufferToPatcher { |patcher|
		var buffer = DmxBuffer();
		patcher.addBuffer(buffer);
	}
	removeBufferFromPatcher { |index, patcher|
/*		var bufkey = patcher.buffers.find([buffer]);*/
		patcher.removeBuffer(index);
	}
	
	addDeviceToBuffer{ |devclass, buffer|
		var theclass = devclass.asSymbol.asClass;
		if(theclass.class.methods[0].argNames.size > 1, {
			this.devOptionsWindow(theclass, buffer);
		}, {
			var device = theclass.new();
			buffer.addDevice(device);
			this.updateView();
		});
	}
	devOptionsWindow { |devclass, buffer|
		var lilwin, argpairs, argpairlayout, addbtn;
		
		lilwin = Window("Arguments for" + devclass.asString, 300@200);
		lilwin.layout = VLayout();
		lilwin.layout.add(
			StaticText().string_("Arguments needed for"+devclass.asString)
			.font_(Font.sansSerif(18, true)), align:\topLeft);
			
		argpairs = [];
		devclass.class.methods[0].argNames.do({ |anarg|
			if(anarg != \this, {
				argpairs = argpairs.add([anarg.asSymbol, TextField()]);
			});
		});
		argpairs.do({ |anarg|
			lilwin.layout.add(HLayout(
				[StaticText().string_(anarg[0].asString), a:\top],
				anarg[1]
			), align:\top);
		});
		
		addbtn = Button().states_([["Add Device to Buffer"]])
			.action_({
				var newclass = devclass.asString ++ ".new(";
				var myargs = [];
				var device;
				argpairs.do({ |pair|
					if(pair[1].notNil, {
						myargs = myargs.add(pair[1].value);
					});
				});
				newclass = newclass ++ myargs.join(',') ++ ")";
				device = newclass.interpret;
				buffer.addDevice(device);
				lilwin.close;
				this.updateView();
			});
		
		lilwin.layout.add(addbtn);
		lilwin.front;
	}
	removeDeviceFromBuffer { |devIndex, buffer|
		buffer.removeDevice(devIndex);
		this.updateView();
	}
	
	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}
}

LGui_manageDevices {
	
	manageDevices { |patcher|
		var window;
		
		
		window.front;
	}
}