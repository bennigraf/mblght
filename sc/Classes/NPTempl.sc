NPTempl {	
	// Node Proxy Templates! Some simple templates for easy Node Proxy Generation...
	classvar templates; // holds different types of devices
	
	var server; // holds default server to create NodeProxys on
	
	*initClass {
		templates = Dictionary();
		templates.add(\panner -> (
			rate: \control,
			init: { |tmpl, args| 
				// panner wants: 'in' (number of inputs), 'out' (number of outputs to pan to)
				if(args.isNil, {
					args = Dictionary();
				});
				if(args[\in].notNil, { tmpl.inChannels = args[\in] }, { tmpl.inChannels = 1});
				if(args[\out].notNil, { 
					tmpl.outChannels = tmpl.inChannels * args[\out]; 
					tmpl.out = args[\out];
				}, { 
					tmpl.outChannels = 2;
					tmpl.out = 1;
				});
			}, 
			source: { |tmpl, args| // returning a source function
				var fn = {
					var in = \in.kr(0!tmpl.inChannels);
/*					var in = SinOsc.kr(1/4)!tmpl.inChannels;*/
					var width = \width.kr(2);
					var ori = \ori.kr(-0.5);
					var pos = \pos.kr(0);
					var inhlpr = { |i|
						PanAz.kr(tmpl.out, in[i], pos, 1, width, ori);
					}!(tmpl.inChannels);
					inhlpr = inhlpr.flop.flatten;
/*					inhlpr.poll;*/
					inhlpr;
				};
				fn;
			}
		));
		templates.add(\tobus -> (
			rate: \control,
			init: { |tmpl, args|
				
			},
			source: { |tmpl, args|
				
			}
		));
	}
		
	*new { | template, args |
		^super.new.init(template, args);
	}
	init { | template, args |
		var node;
		var server = Server.default;
		var tmpl = templates[template];
		if(tmpl.isNil, {
			("No tamplate named" + template + "found!").postln;
			^false;
		});
		tmpl[\init].value(tmpl, args);
		node = NodeProxy(server, tmpl.rate, tmpl.outChannels);
		node.source = tmpl[\source].value(tmpl, args);
		^node;
	}
	
}

/*
Usage:
s.boot;
n = NPTempl(\panner, (\in: 3, \out: 20)) // pans 3 inputs to 20 outputs
n.set(\in, [1, 2, 3])
n.set(\pos, 1.850005)
n.dump
n.bus.dump

*/