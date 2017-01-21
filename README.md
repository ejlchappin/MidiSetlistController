## Introduction
The MidiSetListController enables quick but sophisticated setlists that send particular midi messages to synthesizers or other midi devices, for live performances or studio usages, with intuitive navigation features and advanced midi or keyboard triggers. A key feature is that the setlist can easily shift between presets, but also modify the preset on the fly, for instance enable or disable layered sounds and apply midi control changes. The setlist can be navigated by multiple midi triggers, for instance by configuring a pedal that sends a particular control change message. 

The controller is developed to very quickly make and modify rather complicated setlists, and is robust for a live performance setting. Here is a view of the screen, which has a dashboard on the bottom illustrating the current setting and a display of the past changes.

![screenshot](https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.png)

## Install
No installation is required, the files below are necessary and the configuration needs to be adapted to your midi device.

* Save the [application](https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.jar) to disk
* Save the [midi configuration](https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/midi.txt) to disk
* Save the [setlist](https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/setlist.txt) to disk

The configuration (and setlist) needs to be adjusted for your own midi device (see below). Start the application by double clicking the jar file. If that doesn't work, the Java path should be configured properly on your device. 

## Using the MidiSetlistController
The applicaton opens by loading the midi configuration, the setlist if the files are present in the same folder. Otherwise use the menu items to load a midi config file (CTRL-Q) and a setlist file (CTRL-S). The application attempts to reach the configured midi device and will show you the result. If all works out, the first setlist item is executed.

When a setlist is loaded, you can navigate the setlist. With the arrow keys you move individual settings and page-up and page-down make you move between complete presets. The logic takes care of performing the correct incremental changes, for instance when moving up.

At the bottom of the screen, in large, the current item in the setlist is shown, for a clear view during live performances. For during dark performances, a night colour scheme can be used by CTRL-N. Font sizes can be increased and decreased as well.

## Configuring midi device
The midi device is configured as follows. See the example below for an example (a Roland FA-08).

* A line of the form with the word Device, a comma, and then the actual device name) configures the *midi device*. The midi device is configured by name, which can be found by pressing CTRL-D, which lists all the devices available in the system. This is the format to configure a device:
```
Device,DeviceName
```
* In order to find out how to configure the other parts, simply configure the device name and connect the device. Then press CTRL-M to enable displaying all incoming midi. When pressing notes or changing a presets, the proper messages will be displayed so the commands for midi shortcuts and midi triggers can be easily taken over to the midi configuration and setlist. In case there are long commands, the display can saved to a log (in the file log.txt in the current folder). 
* Midi triggers are configured that the application listens for to trigger changes to the device. This means, no interaction with the computer is necessary during live performance. In the example below, I configured my synth to send a midi control change message 9 on channel 16 with value 127 when I press a pedal. The trigger configures the application to listen to this exact message and executes a Down arrow key, which progresses the setlist. Triggers have the following two forms. The Key refers to the action that is executed when triggered. Options are Up, Down, Pageup, Pagedown, Home and End.

For program and control change messages, type is 'PC' or 'CC', respectively:
```
Trigger,Key,Type,Channel-Data1-Data2
```

For configured midi codes, type 'MC' or system exclusive hex code, type 'SE'
```
Trigger,Key,Type,Value
```

* All lines that don't start with 'Device' or 'Trigger' configure *midi shortcuts*, according to the following format. First you list the shortcut, that you refer to in your setlist. If available, you can add the inverse command shortcut that undo's the change. Otherwise you leave it empty. Finally a System Exclusive Message in hex format is added. Here are a number of commands listed, enabling and disabling patches 1 until 4 and presets 1 and 2 (called studiosets in the Roland FA-08 device).
```
Shortcut,InverseShortcut,Midicode in hex format
``` 

* Lines that start with a semicolon and empty lines are ignored (so the semicolon can be used for comments.

**Example midi.txt:**

```
Device,FA-06 08
Trigger,Down,CC,16-9-127
+1,-1,F0 41 10 00 00 77 12 18 00 40 02 01 25 F7
-1,+1,F0 41 10 00 00 77 12 18 00 40 02 00 26 F7
+2,-2,F0 41 10 00 00 77 12 18 00 41 02 01 24 F7
-2,+2,F0 41 10 00 00 77 12 18 00 41 02 00 25 F7
+3,-3,F0 41 10 00 00 77 12 18 00 42 02 01 23 F7
-3,+3,F0 41 10 00 00 77 12 18 00 42 02 00 24 F7
+4,-4,F0 41 10 00 00 77 12 18 00 43 02 01 22 F7
-4,+4,F0 41 10 00 00 77 12 18 00 43 02 00 23 F7
1,,F0 41 10 00 00 77 12 01 00 00 04 55 00 00 26 F7
2,,F0 41 10 00 00 77 12 01 00 00 04 55 00 01 25 F7
```

Specifying inverse commands makes it possible to move up the setlist within particular presets, by reversing the incremental changes that were done. For instance going up to re-enable patch 1, that was disabled when going down. This makes navigation easier and more fail-safe. It also allows for a consistency check, where you get a warning message if you apply the same code twice in the same preset, without reversing it in between. This is important, because it would be inconsistent if you move up the list of settings.

## Finding your midi codes
The midi code can be found quite easily in the application. When the device is properly connected, press CTRL-M to enable a display of the incoming midi on the screen. If you now play notes/send midi messages you want with your device, you will see the code/preset that can be used to send the same command. Copy the correct messages over (select and right-click to copy with the mouse, or use the log feature from the Display menu bar).

## Making or modyfing a setlist
A setlist starts with a name on the first line for your own reference (see also the example below). Afterwards, lines can have to formats, one for presets and one for incremental changes. The format is comma-separated. When making a change to the setlist on disk, just press CTRL-R to reload the setlist.

With a new preset: 
```
Song name,Place in song,Preset,Sound names (comma-separated)
```

With an incremental change: 
```
Song name,Place in Song,,Midicodes (comma-separated)
```

* The *songname* needs only to be presented when it starts, the application copies it over until it changes.
* The *place in song* is for the user to easily indicate where this setlist item is activated, for instance a measure number or letter or a 'refrain'.
* The *preset* switches a preset that is self-contained, that is, you can switch from any position to another preset and no history of other codes matter. When there is a preset, no incremental changes are read. The rest of the line is reserved for listing the patch sounds that are configured in this preset. This enables the application to display the current sounds also when incremental changes are made later (displaying sound 1 when midicode +1 is executed for instance). 
* The *midi codes* are a comma-separated list of incremental changes within the current preset. Each change is sent seperately to the midi device. When moving up and down the setlist within a preset, incremental changes (or their reverse) are executed, and the preset is not reset. Note that the Preset place-holder remains empty here, so there is a double comma.

Midi codes have different forms. The following types are implemented:
* Midi codes as defined in midi.txt. The codes, 1 and 2, and the incremental change shortcuts (the +1 until +4 and -1 until -4) are midi codes configured for the midi device. 
* System exclusive hex messages:
```
F0 41 10 00 00 77 12 18 00 40 02 01 25 F7
```

* Program change messages:
```
PC-Channel-Program
```

* Control Change: 
```
CC-Channel-Control-Value
```

* Lines that start with a semicolon and empty lines are ignored (so the semicolon can be used for comments.

**Example setlist.txt:**
```
Example setlist

;Song name,Place in song,Preset,Sound names
Basic,,1

My favorite song,,2,Sound name 1,Sound name 2,Sound name3,Sound name 4
;Song name,Place in song,empty,Midicodes
,Verse 1,,+2,-1
,Refrain,,+3,+4
,Bridge,,-2

My next song,,3,Sound name 5,Sound name 6,Sound name 7,Sound name 8
,30,,+1,+2,+3,+4
,31,,-1,-2,-3
,50,,CC-1-11-64
,51,,CC-1-11-30
,70,,PC-1-0
,71,,PC-1-1
```
