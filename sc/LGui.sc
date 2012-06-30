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
				if(ptchrname.value == "", {
					ptchrname.string = "default";
/*					check = false;*/
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
		svbtn = Button().states_([["Save"]])
			.action_({ this.saveStuff });
		
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
	
	saveStuff {
		// save:
		// * patcher - name, connected Buffers, connected Output Devices (with arguments?)
		// * devices - address, group
/*		Patcher.all.at(\default).asCompileString*/
		// as an event with .asCompileString:
/*		(patcher: (name: \default, buffers: (['bufer1', 'buffer2']))).asCompileString*/
		
		var data = ();
		
		var ptchrs = Patcher.all;
		
		data.patchers = List();

		ptchrs.keysValuesDo({ |patcherid, patcher|
			var myPatcher = ();
			var myTempDevices; //
			// get patcher info:
			myPatcher.name = patcherid;
		
			myPatcher.buffers = List();
			patcher.buffers.do({ |buf, n|
				var buffer = (classname: buf.class);
				var devices = buf.devices;
				buffer.devices = List();
				devices.do({ |dev, m|
					var device = ();
					buffer.devices.add(dev.compileString);
				});
				myPatcher.buffers.add(buffer);
			});
			
			myPatcher.devices = List();
			myTempDevices = Dictionary();
			patcher.devices.do({ |dev, n|
				var myDev = (type: dev.device.type, address: dev.device.address);
				myPatcher.devices.add(myDev);
				myTempDevices.add(n -> dev);
			});
			
			myPatcher.groups = List();
			patcher.groups.keysValuesDo({ |grpname, devs|
				var myGrp = (name: grpname, deviceIndizes: List());
				devs.do({ |dev|
					var devindx = myTempDevices.findKeyForValue(dev);
					if(devindx.notNil, {
						myGrp.deviceIndizes.add(devindx);
					});
				});
				myPatcher.groups.add(myGrp);
			});
		
			data.patchers.add(myPatcher);
		});
		
		savedata = data.asCompileString;
		
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
		var bounds = Window.availableBounds;
		
		lilwin = Window("Arguments for" + devclass.asString, Rect(bounds.width/2-150, bounds.height/2+100, 300, 200));
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
	
	var window;
	var updateActions;
	var patcher;
	
	*new { |patcherid|
		^super.new.init(patcherid);
	}
	
	init { |patcherid|
		updateActions = List();
		patcher = Patcher.all.at(patcherid);
		this.manageDevices();
		this.updateView();
	}
	
	manageDevices {
		var window;
		var bounds = Window.availableBounds;
		
		var grpslist, grpAdd, grpRmv;
		
		var devslist;
		var devslctr;
		var addrTxt, autoAddr, addBtn, rmvDevGrp, rmvBtn;
		
		window = Window("Manage Devices", Rect(bounds.width/2-200, bounds.height/2+50, 400, 400));
		
		grpslist = ListView().selectionMode_(\extended)
			.action_({ |list|
				this.updateView;
			});
		updateActions.add({
			var val = grpslist.value ?? 0;
			grpslist.items_(["(none)"] ++ patcher.groupNames).value_(val);
		});
		grpAdd = Button().states_([["Add Group"]])
			.action_({ this.addGroup });
		grpRmv = Button().states_([["Remove Group"]])
			.action_({ 
				if(grpslist.value.notNil && (grpslist.value > 0), {
					patcher.removeGroup(patcher.groupNames.at(grpslist.value - 1));
				});
				this.updateView();
			});
		
		devslist = ListView();
		updateActions.add({
			var items = [];
			var devices;
			if(grpslist.value > 0, {
				devices = patcher.groups[grpslist.items[grpslist.value]];
			}, {
				// all devices if 'none' is selected
				devices = patcher.devices;
			});
			devices.do({ |dev|
				items = items.add(dev.device.type.asString + "- addr:" + dev.device.address);
			});
			devslist.items_(items);
		});
		
		devslctr = PopUpMenu()
			.items_(Device.typeNames().collect({ |name| name.asString+"("++Device.types.at(name).channels++"ch)" }))
			.action_({this.updateView});
		
		addrTxt = TextField();
		updateActions.add({
			if(autoAddr.value, {
				var devType = Device.types[Device.typeNames.at(devslctr.value)];
				addrTxt.string_(patcher.nextFreeAddr(devType.channels));
			});
		});
		autoAddr = CheckBox().value_(true);
		addBtn = Button().states_([["Add Device"]])
			.action_({
				var devTypeName = Device.typeNames.at(devslctr.value);
				var grp = nil;
				if(grpslist.value > 0, { grp = grpslist.items[grpslist.value]; });
				patcher.addDevice(Device(devTypeName, addrTxt.value.asInteger), grp);
				this.updateView();
			});
		rmvDevGrp = Button().states_([["Remove Device from Group"]])
			.action_({
				if(grpslist.value > 0, {
					var group = grpslist.items[grpslist.value];
					var devindex = devslist.value();
					patcher.removeDeviceFromGroup(devindex, group);
				});
				this.updateView;
			});
		rmvBtn = Button().states_([["Remove Device"]])
			.action_({
				var devices, device, group, devIndx;
				if(grpslist.value > 0, {
					devices = patcher.groups[grpslist.items[grpslist.value]];
					group = grpslist.items[grpslist.value];
				}, {
					devices = patcher.devices;
				});	
				device = devices[devslist.value];
				patcher.devices.do({ |dev, n|
					if(dev == device, { devIndx = n });
				});
/*				devIndx = patcher.devices.find([device]);*/
/*				if(group.notNil, { patcher.removeDeviceFromGroup(devslist.value, group) });*/
				patcher.removeDevice(devIndx);
				this.updateView();
			});

		window.layout = VLayout(
			StaticText().string_("Groups:").font_(Font.sansSerif(18, true)),
			grpslist,
			HLayout(grpAdd, grpRmv),
			StaticText().string_("Devices:").font_(Font.sansSerif(18, true)),
			devslist,
			StaticText().string_("Add Device:").font_(Font.sansSerif(18, true)),
			HLayout(StaticText().string_("Device-type:"), devslctr),
			HLayout([StaticText().string_("Address:"), s:1], [addrTxt, s:1]),
			HLayout(StaticText().string_("Auto-Address:"), [autoAddr, a:\right]),
			addBtn,
			HLayout(rmvDevGrp, rmvBtn)
		);
		window.front;
	}
	
	addGroup {
		var dialog;
		var bounds = Window.availableBounds;
		var addbtn, grpname;

		dialog = Window("Add Patcher", Rect(bounds.width/2-200, bounds.height/2+50, 400, 100));

		grpname = TextField();

		addbtn = Button().states_([["Create Group"]])
			.action_({ |btn|
				var check = true;
				patcher.groupNames.do({ |name|
					if(name == grpname.string, {
						check = false;
						"Group exists already!".postln;
					});
				});
				if(grpname.string == "", {
					check = false;
					"No Groupname given!".postln;
				});
				if(check, {
					dialog.close;
					patcher.addGroup(grpname.string);
					this.updateView();
				});
			});

		dialog.layout_(VLayout(
			HLayout(
				StaticText().string_("Group name:"),
				grpname
			),
			[addbtn, a:\topleft]
		));

		dialog.front;
	}

	updateView {
		// actions to update the view...
		updateActions.do({ |ua|
			ua.value();
		});
	}
}