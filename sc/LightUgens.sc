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
		
		
