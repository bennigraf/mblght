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