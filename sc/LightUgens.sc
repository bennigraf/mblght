Hsv2rgb : UGen {
/*    *ar { arg freq = 440.0, iphase = 0.0;
        ^this.multiNew('audio', freq, iphase)
    }*/
    *kr { arg h = 0.0, s = 0.0, v = 0.0;
		var rgb = DC.kr!3;
		var rgbhlpr = DC.kr!3;
		var hsv = [h, s, v];
		rgb = Select.kr(hsv[1].ceil, [ // 0 or 1, 0 means no color (no saturation)
			hsv[2]!3;
		, {
			rgbhlpr[0] = hsv[2] * (1 - hsv[1]);
			rgbhlpr[1] = hsv[2] * (1 - (hsv[1] * ((hsv[0] * 6) - (hsv[0] * 6).floor)));
			rgbhlpr[2] = hsv[2] * (1 - (hsv[1] * (1 - ((hsv[0] * 6) - (hsv[0] * 6).floor))));
			Select.kr((hsv[0]*6).floor, [
				{ [hsv[2], rgbhlpr[2], rgbhlpr[0]] },
				{ [rgbhlpr[1], hsv[2], rgbhlpr[0]] },
				{ [rgbhlpr[0], hsv[2], rgbhlpr[2]] },
				{ [rgbhlpr[0], rgbhlpr[1], hsv[2]] },
				{ [rgbhlpr[2], rgbhlpr[0], hsv[2]] },
				{ [hsv[2], rgbhlpr[0], rgbhlpr[1]] },
			]);
		}]);
		^rgb;
    }
}


/*Rotator.dump.kr*/
/*Rotator.kr(20, 3)*/
Rotator : UGen {
	
	classvar <rate = \control;
	
	*kr { 
		arg channels = 10,
			group = 1,
			in = [],
			rotation = 0.0,
			width = 2.2,
			lag = 1;
			
		var numChannels, combinedChannels, out, pannedchans;
		
		numChannels = channels;
		combinedChannels = { |chanNum| 
			var out = [];
			group.do{ |n|
				out = out.add(in[chanNum * group + n]);
			};
			out;
		}!numChannels;
/*		combinedChannels.dump;*/
		
		out = 0!(channels * group);
		pannedchans = combinedChannels.collect({ |chan|
			// chan = [r, g, b]
			// -> [[r1, r2, r3, ..], [g1, g2, g3, ..], ..]
			PanAz.kr(channels, chan, rotation, 1, width);
		});
		// pannedchans now has numChannels elements with group elements with numChannels elements each...

		out.size.do({ |outChanNum|
			var sum = 0;
			numChannels.do({ |j|
				sum = (sum + pannedchans[j][outChanNum%group].wrapAt((outChanNum/group).floor - j));
			});
			out[outChanNum] = sum;
		});
		
		^out.lag(lag); // smothes pannings a bit
	}
}
		
MultiPanAz : UGen {
	classvar <rate = \control;
	
	*kr {
		arg channels = 2,
			in = [],
			pos = 0,
			width = 2,
			ori = -0.5;
			
		var hlpr;
		var inChannels = in.size;
		
		hlpr = { |i|
			PanAz.kr(channels, in[i], pos, 1, width, ori);
		}!inChannels;
		hlpr = hlpr.flop.flatten;
		
		^hlpr;
	}
}		

Mirror : UGen {
	classvar <rate = \control;
	
	*kr {
		arg channels = 2,
			groups = 3,
			ins = [],
			wet = 1; // 0 dry, 1 wet
		
		var outs = 0!60;
		
		channels.do{ |i|
			var ofst = (19-i)*3;
			groups.do({|j| 
				var orign = ins[i*3+j];
				var mirr = ins[ofst+j];
				outs[i*3+j] = (1 - wet * orign) + (wet * mirr);
			});
		};
		^outs;
	}
}

Blitzen : UGen {
	classvar <rate = \control;
	
	*kr {
		arg channels = 10, groups = 3,
			rate = 1,
			pos = 0,
			width = 0,
			in;
		
		// spread applys sine-like verteilungsglocke over all channels which can be moved
		// around with pos by moving it's phase and it's width is controlled by pow(width),
		// which 'overdrives' the whole thing (or underdrives for width < 1).
		
		// offst = 0..2
		var spread = { |offst| SinOsc.kr(0, offst * pi/2, 0.5, 0.5) ** width.lincurve(0, channels, 0, 100, 8) };
		var trigs = { |n| 
			var amp = spread.value(1/channels*n*2);
			var freq = rate * amp;
			Dust.kr(freq) * spread.value(1/channels*n*2);
		}!channels;
		
		var carrier = Trig1.kr(trigs, dur: 0.1);

		var sig, out = 0!(channels*groups);
		// ins: 'white' (if none), else use dust as 'amp'
		if(in.isNil, {
			sig = DC.kr(1)!groups;
		}, {
			// check if in signal is an array, could also be a float
			if(in.size > 0, {
				sig = in;
			}, {
				sig = [in];
			});
		});
		
		out = { |n|
			out[n] = carrier[(n/groups).floor.asInteger];
/*			out[n] = sig.wrapAt[n] * carrier[(n/groups).floor.asInteger];*/
		}!(channels * groups);
		
		^out;
	}
}
