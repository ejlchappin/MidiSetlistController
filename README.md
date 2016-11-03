# Introduction
The MidiSetListController enables quick but sophisticated setlists with elements that sends particular midi messages to synthesizers or other midi devices, for live performances or studio usages, with intuitive navigation features.

A key feature is that the setlist can easily shift between presets, but also by modifying the preset on the fly, for instance enable or disable layered sounds.

# Install

Save the application to disk: https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.jar 
Save the setlist to disk: https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/setlist.ini
Save the midi configuration to disk: https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/config.ini
The configuration may need to be adjusted for your own midi device (see below).

Start command-line/terminal and navigate to the folder
Start the application with:
java -jar MidiSetlistController.jar

# Using the MidiSetlistController

The applicaton opens with the menu, which can always be recalled by pressing M. If the config.ini and setlist.ini files are present in the same folder they are loaded. Otherwise use the key F to load a config file and G to load a setlist file. The application attempts to reach the configured midi device and will show you the result. 

When a setlist is loaded, you can navigate the setlist. With the arrow keys you move individual settings and page-up and page-down make you move between complete presets. The logic takes care of performing the correct incremental changes, for instance when moving up. 

At the bottom of the screen, in large, the current item in the setlist is shown, for a clear view during live performances.

# Configuring midi device

The midi device is configured as follows. See the example excerpt below.

The first line selects the midi device. The midi device is configured by name, which can be found by pressing D, which lists all the devices available in the system. In may case it is a Roland FA-08, which is listed as FA-06 08. The name is put on the first line of the config.ini file.

The second line is reserved for the pedal command which is done by control change signal, i.e. the channel, control change and value. These three are put, comma-separated, in this order.

From the third line midi messages are configured. First the shortcut, that is the shortcut used in your setlist, the System Exclusive Message formatted in 7bit bytes, and the reverse command shortcut, if that is available. Here are two commands listed, the first enabling patch 1 and the second disabling patch 1. These are each others reverse, so the third element contains its reverse.  

FA-06 08
16,9,127
+1,240 65 16 0 0 119 18 24 0 64 2 1 37 247,-1
-1,240 65 16 0 0 119 18 24 0 64 2 0 38 247,+1

# Finding your midi codes
The midi code can be found quite easily in the application. Press I to enable the midi receiver printing it's output to the screen. If you now play notes/send midi messages you want with your device, you will see the result. Copy the correct system exclusive message over, and translate the Hex code to a 7bit decimal version in this webpage http://mididesigner.com/help/midi-byte-calculator/ (enter the code under Bytes to values, and find the right code in the individually listed decimal numbers under Result). On the TODO list is to do the conversion in the application and allow a Hex midi code directly.

# Making or modyfing a setlist
