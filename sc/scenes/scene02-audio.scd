

t=Server.internal

ServerOptions.devices
t.options.device_("Agg")
t.boot
t.quit

t.recChannels_(4);
t.recSampleFormat_("int16");
t.prepareForRecord
t.record
t.stopRecording