
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
		// a few keys are reserved for special purposes:
		//  channel - holds to total number of dmx channels used by this device
		//  numArgs - holds number of arguments each method expects
		//  init - holds the init method which i.e. can set some default values
		Device.addType(\dim, (
			channels: 1,
			numArgs: (dim: 1),
			dim: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0] * 255).round.asInteger);
			},
			init: { |self|
				self.setDmx(0, 0);
			}
		));
		Device.addType(\camera, (
			channels: 16,
			numArgs: (camerapos: 3, cameraaim: 3),
			camerapos: { |self, args|
				// set pos (x, y, z) and direction (pan, title – no pitch for now) if wanted
				// split x, y, z to msb/lsb (coarse/fine)
				var x = [(args[0] * 255).floor, (args[0] * 255 % 1 * 255).round];
				var y = [(args[1] * 255).floor, (args[1] * 255 % 1 * 255).round];
				var z = [(args[2] * 255).floor, (args[2] * 255 % 1 * 255).round];

				self.setDmx(0, x[0]);
				self.setDmx(1, x[1]);
				self.setDmx(2, y[0]);
				self.setDmx(3, y[1]);
				self.setDmx(4, z[0]);
				self.setDmx(5, z[1]);
			},
			cameraaim: { |self, args|
				// set pan/tilt to where the camera should look to...
				// args: aimx, aimy, aimz
				var pans, tilts;
				
				// get current camera position:
				var dmx = self.getDmx;
				var xcam = dmx[0] / 255 + (dmx[1] / 255 / 255);
				var ycam = dmx[2] / 255 + (dmx[3] / 255 / 255);
				var zcam = dmx[4] / 255 + (dmx[5] / 255 / 255);
				
				// calculate stuff. 
				// 1. distance on x/z-plane using pythagoras
				var d = ((xcam - args[0])**2 + ((zcam - args[2])**2)).sqrt;
				// 2. "height"-difference (delta y)
				var h = ycam - args[1];
				// 3. tilt from 1. and 2.
				var tilt = (h / d).atan;
				// 4. pan: delta x / delta z...
				// but counter-clockwise!?
				var dx = xcam - args[0];
				var dz = zcam - args[2];
				var pan = (dx / dz).atan * -1;

				if(dz < 0, {
					pan = pan + pi;
				});

				pan = pan.wrap(0, 2pi);
				tilt = tilt.wrap(0, 2pi);
								
				tilts = [(tilt/2pi * 255).floor, (tilt/2pi * 255 % 1 * 255).round];
				pans = [(pan/2pi * 255).floor, (pan/2pi * 255 % 1 * 255).round];
				
				self.setDmx(6, pans[0]);
				self.setDmx(7, pans[1]);
				self.setDmx(8, tilts[0]);
				self.setDmx(9, tilts[1]);
			},
			init: { |self|
				self.setDmx(0, 128);
				self.setDmx(1, 0);
				self.setDmx(2, 129);
				self.setDmx(3, 0);
				self.setDmx(4, 140);
				self.setDmx(5, 0);
				
				self.setDmx(13, 20); // ambient light
				self.setDmx(14, 128); // overall light?? alpha-value??
				self.setDmx(15, 40); // "athmosphere" -> foggyness
			}
		));
		Device.addType(\smplrgb, (
			channels: 3,
			numArgs: (color: 3),
			color: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0] * 255).round.asInteger);
				self.setDmx(1, (args[1] * 255).round.asInteger);
				self.setDmx(2, (args[2] * 255).round.asInteger);
			}
		));
		Device.addType(\waldpar, (
			channels: 6,
			numArgs: (color: 3, strobe: 1),
			color: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0] * 255).round.asInteger);
				self.setDmx(1, (args[1] * 255).round.asInteger);
				self.setDmx(2, (args[2] * 255).round.asInteger);
			},
			init: { |self|
				self.setDmx(3, 0); // color-shifter??
				self.setDmx(4, 0); // shutter
				self.setDmx(5, 0); // macro
			},
			strobe: { |self, args|
				self.setDmx(4, (args[0] * 255).round.asInteger);
			}
		));
		Device.addType(\waldfuck2, (
			channels: 7,
			// dim, strobe, r, g, b, w, chaser, chaser2
			numArgs: (color: 3),
			color: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(2, (args[0] * 255).round.asInteger);
				self.setDmx(3, (args[1] * 255).round.asInteger);
				self.setDmx(4, (args[2] * 255).round.asInteger);
			},
			init: { |self|
				self.setDmx(0, 255); // dimmer
				self.setDmx(1, 0); // strobe
				self.setDmx(5, 0); // w
				self.setDmx(6, 0); // chaser
			}
		));
		Device.addType(\waldfuck, (
			channels: 6,
			numArgs: (color: 3),
			color: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(1, (args[0] * 255).round.asInteger);
				self.setDmx(2, (args[1] * 255).round.asInteger);
				self.setDmx(3, (args[2] * 255).round.asInteger);
			},
			init: { |self|
				self.setDmx(0, 255); // dimmer
				self.setDmx(4, 0); // macro
				self.setDmx(5, 0); // strobe
			}
		));
		Device.addType(\waldbarInit, (
			channels: 11,
			numArgs: (),
			init: { |self|
				// set intensity of whole bar to FL
				self.setDmx(10, 255);
			}
		));
		Device.addType(\waldbar, (
			channels: 3,
			numArgs: (color: 3),
			color: { |self, args|
				self.setDmx(0, (args[0] * 255).round.asInteger);
				self.setDmx(1, (args[1] * 255).round.asInteger);
				self.setDmx(2, (args[2] * 255).round.asInteger);
			}
		));
		Device.addType(\waldfog, (
			channels: 1,
			numArgs: (fog: 1),
			fog: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0]*255).round.asInteger);
			},
			init: {|self| self.setDmx(0, 0) }
		));
		Device.addType(\waldblitz, (
			channels: 2,
			numArgs: (blitz: 1),
			blitz: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0] * 255).round.asInteger);
			},
			init: { |self| 
				self.setDmx(0, 255);
				self.setDmx(1, 255); // set intens to fl
			}
		));
		Device.addType(\robeCw1200E, (
			channels: 17,
			numArgs: (color: 3, cmyk: 4, strobe: 1, zoom: 1),
			init: { |self|
				// pan/tilt center:
				self.setDmx(0, 127);
				self.setDmx(2, 127);
				// shutter open:
				self.setDmx(15, 255);
				// white/poweron/intensity:
				self.setDmx(16, 255);
				// zoom: narrowest...
				self.setDmx(14, 0);
			},
			color: { |self, rgb|
				// rgb 2 cmyk:
				var cmyk = [0, 0, 0, 0];
				rgb = rgb.clip(0, 1); // clip incoming...
				// another try: http://stackoverflow.com/questions/2426432/convert-rgb-color-to-cmyk
				//Black   = minimum(1-Red,1-Green,1-Blue)
				//Cyan    = (1-Red-Black)/(1-Black)
				//Magenta = (1-Green-Black)/(1-Black)
				//Yellow  = (1-Blue-Black)/(1-Black)
				cmyk[3] = (1.0-rgb).minItem;
				cmyk[0] = (1 - rgb[0] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[1] = (1 - rgb[1] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[2] = (1 - rgb[2] - cmyk[3]) / (1 - cmyk[3]);
				
				// set cmyk...
				self.setDmx(8, (cmyk[0] * 255).round.asInteger);
				self.setDmx(9, (cmyk[1] * 255).round.asInteger);
				self.setDmx(10, (cmyk[2] * 255).round.asInteger);
				self.setDmx(16, 255 - (cmyk[3] * 255).round.asInteger); // k is intensity 'inversed'
			},
			// careful! Don't write multiple actions that overwrite each other! Oh noes...
/*			cmyk: { |self, cmyk|
				cmyk = (cmyk * 255).round.asInteger;
				self.setDmx(8, cmyk[0]);
				self.setDmx(9, cmyk[1]);
				self.setDmx(10, cmyk[2]);
				self.setDmx(16, 255 - cmyk[3]); // k is intensity 'inversed'
			},*/
			strobe: { |self, strobe|
				if(strobe[0] == 0, {
					self.setDmx(15, 255);
				}, {
					self.setDmx(15, (strobe[0] * 254).round.asInteger);
				});
			},
			zoom: { |self, zoom|
				self.setDmx(14, (zoom[0] * 255).round.asInteger);
			}
		));
		Device.addType(\ClrChngr, (
			channels: 11,
			numArgs: (color: 3, cmyk: 4),
			init: { |self|
				// shutter open:
				self.setDmx(4, 255);
				// white/poweron/intensity:
				self.setDmx(5, 255);
				// zoom: 
				self.setDmx(7, 255);
			},
			color: { |self, rgb|
				// rgb 2 cmyk:
				var cmyk = [0, 0, 0, 0];
				rgb = rgb.clip(0, 1); // clip incoming...
				// another try: http://stackoverflow.com/questions/2426432/convert-rgb-color-to-cmyk
				//Black   = minimum(1-Red,1-Green,1-Blue)
				//Cyan    = (1-Red-Black)/(1-Black)
				//Magenta = (1-Green-Black)/(1-Black)
				//Yellow  = (1-Blue-Black)/(1-Black)
				cmyk[3] = (1.0-rgb).minItem;
				cmyk[0] = (1 - rgb[0] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[1] = (1 - rgb[1] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[2] = (1 - rgb[2] - cmyk[3]) / (1 - cmyk[3]);

				// set cmyk...
				self.setDmx(1, (cmyk[0] * 255).round.asInteger);
				self.setDmx(2, (cmyk[1] * 255).round.asInteger);
				self.setDmx(3, (cmyk[2] * 255).round.asInteger);
				self.setDmx(5, 255 - (cmyk[3] * 255).round.asInteger); // k is intensity 'inversed'
			},
			// careful! Don't write multiple actions that overwrite each other! Oh noes...
/*			cmyk: { |self, cmyk|
				cmyk = (cmyk * 255).round.asInteger;
				self.setDmx(8, cmyk[0]);
				self.setDmx(9, cmyk[1]);
				self.setDmx(10, cmyk[2]);
				self.setDmx(16, 255 - cmyk[3]); // k is intensity 'inversed'
			},*/
		));
		Device.addType(\waldStudio, (
			channels: 7,
			numArgs: (color: 3, strobe: 1),
			init: { |self|
				self.setDmx(6, 255); // dimmer
				fork{
					self.setDmx(0, 255);
					self.setDmx(1, 255);
					self.setDmx(2, 255);
					1.wait;
					self.setDmx(0, 0);
					self.setDmx(1, 0);
					self.setDmx(2, 0);
				};
				self.setDmx(3, 0); // macrostuff
				self.setDmx(4, 0);
				self.setDmx(5, 0);
			},
			color: { |self, rgb|
				self.setDmx(0, (rgb[0] * 255).round.asInteger);
				self.setDmx(1, (rgb[1] * 255).round.asInteger);
				self.setDmx(2, (rgb[2] * 255).round.asInteger);	
			},
			strobe: { |self, strobe|
				
			}
			
		));
	}
	
	*new { |mytype, myaddress = 0|
		^super.new.init(mytype, myaddress);
	}
	init { | mytype, myaddress = 0 |
		var channels;
		address = myaddress;
		type = mytype;
		channels = types[type][\channels];
		dmxData = List.newClear(channels).fill(0);
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
	*typeNames {
		var myTypes = [];
/*		types.postln;*/
		types.keysValuesDo({|name, dev|
			myTypes = myTypes.add(name);
		});
		^myTypes;
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
			// calls back setDmx itself...
			def.at(method.asSymbol).value(this, arguments);
		}, {
			("method "+method+" not found in "+type.asString+"!").postln;
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
	color: { |this, args|
		var r = args[0];
		var g = args[1];
		var b = args[2];
		this.setDmx(addr, r);
		this.setDmx(addr+1, g);
		this.setDmx(addr+2, b);
	},
	strobe: { |this, onoff|
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
// though message now uses event-syntax...

*/