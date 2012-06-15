
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
			color: { |self, args|
				// return list with dmx slots/addresses (starting from 0 for this device) and values
				self.setDmx(0, (args[0] * 255).round.asInteger);
				self.setDmx(1, (args[1] * 255).round.asInteger);
				self.setDmx(2, (args[2] * 255).round.asInteger);
			}
		));
		Device.addType(\robeCw1200E, (
			channels: 17,
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
				
				// another try: http://stackoverflow.com/questions/2426432/convert-rgb-color-to-cmyk
				//Black   = minimum(1-Red,1-Green,1-Blue)
				//Cyan    = (1-Red-Black)/(1-Black)
				//Magenta = (1-Green-Black)/(1-Black)
				//Yellow  = (1-Blue-Black)/(1-Black)
				
				cmyk[3] = rgb.minItem;
				cmyk[0] = (1 - rgb[0] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[1] = (1 - rgb[1] - cmyk[3]) / (1 - cmyk[3]);
				cmyk[2] = (1 - rgb[2] - cmyk[3]) / (1 - cmyk[3]);
				
				// set cmyk...
				self.setDmx(8, (cmyk[0] * 255).round.asInteger);
				self.setDmx(9, (cmyk[1] * 255).round.asInteger);
				self.setDmx(10, (cmyk[2] * 255).round.asInteger);
				self.setDmx(16, 255 - (cmyk[3] * 255).round.asInteger); // k is intensity 'inversed'
			},
			cmyk: { |self, cmyk|
				cmyk = (cmyk * 255).round.asInteger;
				self.setDmx(8, cmyk[0]);
				self.setDmx(9, cmyk[1]);
				self.setDmx(10, cmyk[2]);
				self.setDmx(16, 255 - cmyk[3]); // k is intensity 'inversed'
			},
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