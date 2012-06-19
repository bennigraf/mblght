// src: http://www.csgnetwork.com/csgcolorsel4.html
// Another one (better): http://snipplr.com/view/14590/

function RGB2HSV (RGB, HSV) {
    r = RGB.r / 255; g = RGB.g / 255; b = RGB.b / 255; // Scale to unity.

    var minVal = Math.min(r, g, b);
    var maxVal = Math.max(r, g, b);
    var delta = maxVal - minVal;

    HSV.v = maxVal;

    if (delta == 0) {
        HSV.h = 0;
        HSV.s = 0;
    } else {
        HSV.s = delta / maxVal;
        var del_R = (((maxVal - r) / 6) + (delta / 2)) / delta;
        var del_G = (((maxVal - g) / 6) + (delta / 2)) / delta;
        var del_B = (((maxVal - b) / 6) + (delta / 2)) / delta;

        if (r == maxVal) {HSV.h = del_B - del_G;}
        else if (g == maxVal) {HSV.h = (1 / 3) + del_R - del_B;}
        else if (b == maxVal) {HSV.h = (2 / 3) + del_G - del_R;}
        
        if (HSV.h < 0) {HSV.h += 1;}
        if (HSV.h > 1) {HSV.h -= 1;}
    }
    HSV.h *= 360;
    HSV.s *= 100;
    HSV.v *= 100;
}

function HSV2RGB (HSV, RGB) {
    var h = HSV.h / 360; var s = HSV.s / 100; var v = HSV.v / 100;
    if (s == 0) {
        RGB.r = v * 255;
        RGB.g = v * 255;
        RGB.b = v * 255;
    } else {
        var_h = h * 6;
        var_i = Math.floor(var_h);
        var_1 = v * (1 - s);
        var_2 = v * (1 - s * (var_h - var_i));
        var_3 = v * (1 - s * (1 - (var_h - var_i)));
        
        if (var_i == 0) {var_r = v; var_g = var_3; var_b = var_1}
        else if (var_i == 1) {var_r = var_2; var_g = v; var_b = var_1}
        else if (var_i == 2) {var_r = var_1; var_g = v; var_b = var_3}
        else if (var_i == 3) {var_r = var_1; var_g = var_2; var_b = v}
        else if (var_i == 4) {var_r = var_3; var_g = var_1; var_b = v}
        else {var_r = v; var_g = var_1; var_b = var_2};
        
        RGB.r = var_r * 255;
        RGB.g = var_g * 255;
        RGB.b = var_b * 255;
    }
}
(
f = {
	var rgb = Silent.ar!3;
	var rgbhlpr = Silent.ar!3;
	var hsv = [DC.ar(0), DC.ar(1), DC.ar(1)];
	hsv = [MouseX.kr(0, 1), MouseY.kr(0, 1), \val.ar(1)];
	rgb = Select.ar(hsv[1].ceil, [ // 0 or 1, 0 means no color (no saturation)
		hsv[2]!3;
	, {
		rgbhlpr[0] = hsv[2] * (1 - hsv[1]);
		rgbhlpr[1] = hsv[2] * (1 - (hsv[1] * ((hsv[0] * 6) - (hsv[0] * 6).floor)));
		rgbhlpr[2] = hsv[2] * (1 - (hsv[1] * (1 - ((hsv[0] * 6) - (hsv[0] * 6).floor))));
		Select.ar((hsv[0]*6).floor, [
			{ [hsv[2], rgbhlpr[2], rgbhlpr[0]] },
			{ [rgbhlpr[1], hsv[2], rgbhlpr[0]] },
			{ [rgbhlpr[0], hsv[2], rgbhlpr[2]] },
			{ [rgbhlpr[0], rgbhlpr[1], hsv[2]] },
			{ [rgbhlpr[2], rgbhlpr[0], hsv[2]] },
			{ [hsv[2], rgbhlpr[0], rgbhlpr[1]] },
		]);
	}]);
	SendReply.ar(Impulse.ar(25), '/clr', rgb);
	Silent.ar();
}.play
)
s.boot;
(
f = { 
	var rgb;
	var hsv = [[1, 0.4] * MouseX.kr(0, 1), MouseY.kr(0, 1), \val.kr(1)];
	rgb = Hsv2rgb.kr(hsv[0], hsv[1], hsv[2]).flop.flatten;
/*	SendReply.ar(Impulse.ar(25), '/clr', rgb);*/
	rgb.poll;
}.play)
f.free
OSCdef(\clr, { |msg|
	defer{ w.background_(Color(msg[3], msg[4], msg[5]))}
}, '/clr');

w = Window()
w.front
x = Slider(w)
x.action = { |slidr| f.set(\val, slidr.value())}
