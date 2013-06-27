Hsv2rgb : UGen {
/*    *ar { arg freq = 440.0, iphase = 0.0;
        ^this.multiNew('audio', freq, iphase)
    }*/
    *kr { arg h = 0.0, s = 0.0, v = 0.0;
		var rgb = DC.kr!3;
		var rgbhlpr = DC.kr!3;
		var hsv = [h.wrap(0, 1), s.clip(0, 1), v.clip(0, 1)];
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

Cmyk2rgb : UGen {
/*    *ar { arg freq = 440.0, iphase = 0.0;
        ^this.multiNew('audio', freq, iphase)
    }*/
    *kr { arg c = 0.0, m = 0.0, y = 0.0, k = 0.0;
		var rgb = DC.kr!3;
		// after "http://www.rapidtables.com/convert/color/cmyk-to-rgb.htm"
		rgb[0] = (1-c) * (1-k);
		rgb[1] = (1-m) * (1-k);
		rgb[2] = (1-y) * (1-k);
		
		^rgb;
    }
}


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
		
		^out.lag(lag); // smoothes pannings a bit
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
		
		var outs = 0!(channels*groups);
		
		channels.do{ |i|
			var ofst = (channels-1-i)*groups;
			groups.do({|j| 
				var orign = ins[i*groups+j];
				var mirr = ins[ofst+j];
				outs[i*groups+j] = (1 - wet * orign) + (wet * mirr);
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
			in,
			ori = -0.5;
		
		// spread applys sine-like verteilungsglocke over all channels which can be moved
		// around with pos by moving it's phase and it's width is controlled by pow(width),
		// which 'overdrives' the whole thing (or underdrives for width < 1).
		
		// offst = 0..2
		var spread = { |offst| 
			var myOri = (pi/channels*ori*2) * -1;
			SinOsc.kr(0, offst * pi +(pi/2) +myOri, 0.5, 0.5) ** width.lincurve(0, channels, 100, 0, -8) };
			// 				\-- offset between 0 and 2
			// 							\-- moves whole bell shape so that max is on 0 (cosine-like!)
			//									\-- moves bell shape like ori in PanAz
		var trigs = { |n| 
			var amp = spread.value(1/channels*n*2 + pos);
			var freq = rate * amp;
			Dust.kr(freq) * amp;
		}!channels;
		

/*		var carrier = Trig.kr(trigs, dur: 0.051);*/
		var carrier = Decay.kr(Trig.kr(trigs, dur: 0.024), 0.05);

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
			sig.wrapAt(n) * carrier[(n/groups).floor.asInteger];
		}!(channels * groups);
		^out;
	}
}



Pan2d {
	classvar <rate = \control;
	
	*kr {
		arg gridx = 8,
			gridy = 8,
			in,
			posx = 0,
			posy = 0,
			width = 0;

		// assuming grid outputs stuff l2r t2b 
		// posx/posy (-1 to 1) is actually the phase of a sine (0 to pi) that's spread
		// over the whole grid

		var outs = 0!(gridx * gridy);

		width = width.linexp(0, 1, 50, 1);

		posx = posx.linlin(-1, 1, -pi/2, pi/2);
		posy = posy.linlin(-1, 1, -pi/2, pi/2);

		gridy.do{ |j|
			gridx.do{ |i|
				outs[(j * gridy) + i] = in
				* (pi/gridx * i + posx ).sin.clip(0, 1)
				* (pi/gridy * j + posy ).sin.clip(0, 1)
				** width;
			}
		};
		
		^outs;
	}
		
}