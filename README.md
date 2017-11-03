## Introduction
The MidiSetListController enables quick but sophisticated setlists that send particular midi messages to synthesizers or other midi devices, for live performances or studio usages, with intuitive navigation features and advanced midi or keyboard triggers. A key feature is that the setlist can easily shift between presets, but also modify the preset on the fly, for instance enable or disable layered sounds, apply midi control changes, and play sample wave files. The setlist can be navigated by multiple midi triggers, for instance by configuring a pedal that sends a particular control change message. 

The controller is developed to very quickly make and modify rather complicated setlists, and is robust for a live performance setting. Here is a view of the screen, which has a dashboard on the bottom illustrating the current setting and a display of the past changes.

![screenshot](https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.png)

## Install
No installation is required, only the files below are necessary. The configuration file needs to be adapted to your midi device.

* Save the [application](https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.jar) to disk
* Save the [midi configuration](https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/midi.txt) to disk
* Save the [setlist](https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/setlist.txt) to disk

The configuration needs to be adjusted for your own midi device (see below). Start the application by double clicking the jar file. If that doesn't work, the Java path should be configured properly on your device. 

## Using the MidiSetlistController
The applicaton opens by loading the midi configuration, the setlist if the files are present in the same folder. Otherwise use the menu items to load a midi config file (CTRL-Q) and a setlist file (CTRL-S). The application attempts to reach the configured midi device and will show you the result. If all works out, the first setlist item is executed. If the device is not available (or not well configured) you can still use the application, but the dashboard will be shown in red and give error messages.

When a setlist is loaded, you can navigate the setlist. With the space key you proceed to the next item. Arrow keys also allow you to move between individual settings, but are designed for navigation and will ignore wave samples and wait commands. Page-up and page-down make you move between complete presets. The logic takes care of performing the correct incremental changes, for instance when moving up.

At the bottom of the screen, in large, the current item in the setlist is shown, for a clear view during live performances. For during dark performances, a night colour scheme can be used by CTRL-N. Font sizes can be increased and decreased as well. The bottom of the screen turns red if the midi device becomes unavailable.

## Configuring midi device
The midi device is configured as follows. See the example below for an example (a Roland FA-08).

* A line of the form with the word Device, a comma, and then the actual device name) configures the *midi device*. The midi device is configured by name, which can be found by pressing CTRL-D, which lists all the devices available in the system. This is the format to configure a device:
```
Device,DeviceName
```
* In order to find out how to configure the other parts, simply configure the device name and connect the device. Then press CTRL-M to enable displaying all incoming midi. When pressing notes or changing a presets, the proper messages will be displayed so the commands for midi shortcuts and midi triggers can be easily taken over to the midi configuration and setlist. In case there are long commands, the display can saved to a log (in the file log.txt in the current folder). 
* Midi triggers are configured that the application listens for to trigger changes to the device. This implies that no interaction with the computer is necessary during a live performance. In the example below, the synth is configured to send a midi control change message 9 on channel 16 with value 127 when a particular connected pedal is pressed. The trigger configures the application to listen to this exact message and executes a Space key, which progresses the setlist. Triggers have the following two forms. The Key refers to the action that is executed when triggered. Options are Space, Up, Down, Pageup, Pagedown, Home and End. A bluetooth pedal can also be used. Simply configure the pedal to be the Space key; this requires no configuration in the application.

For program and control change messages, type is 'PC' or 'CC', respectively:
```
Trigger,Key,Type,Channel-Data1-Data2
```

For configured midi codes, type 'MC' or system exclusive hex code, type 'SE'
```
Trigger,Key,Type,Value
```

* All lines that don't start with 'Device' or 'Trigger' configure *midi shortcuts*, according to the following format. First you list the shortcut, that you refer to in your setlist. If available, you can add the inverse command shortcut that undo's the change. Otherwise you leave it empty. Finally a System Exclusive Message in hex format is added. Here are a number of commands listed, selecting presets (called studiosets in the Roland FA-08 device), selecting parts, enabling and disabling parts, muting and unmuting parts (which is something different on this device), and selecting the key range for channel 1. The =-sign is in the setlist replaced with a number. The according number is added or subtracted in the hex command. This allows to configure one command for all presets. Also individual commands can be added, without the =-sign.
```
Shortcut,InverseShortcut,Midicode in hex format
``` 

* Lines that start with a semicolon and empty lines are ignored (so the semicolon can be used for comments.

**Example midi.txt:**

```
Device,FA-06 08
Trigger,Space,CC,16-9-127
;Switching User studiosets (s1-s128)
s=,,F0 41 10 00 00 77 12 01 00 00 04 55 00 7F+= 27-= F7

;Part select 1-16 (p1-p16)
p=,p16,F0 41 10 00 00 77 12 18 00 00 54 7F+= 15-= F7

;Enabling parts 1-16
+=,-=,F0 41 10 00 00 77 12 18 00 3F+= 02 01 26-= F7

;Disabling parts 1-16
-=,+=,F0 41 10 00 00 77 12 18 00 3F+= 02 00 27-= F7

;Muting parts
M=,m=,F0 41 10 00 00 77 12 18 00 1F+= 25 01 23-= F7

;Unmuting parts
m=,M=,F0 41 10 00 00 77 12 18 00 1F+= 25 00 24-= F7

;Lower key per channel, notes 1-128
l01:=,,F0 41 10 00 00 77 12 18 00 40 00 7F+= 29-= F7

;Upper key per channel, notes 1-128
u01:=,,F0 41 10 00 00 77 12 18 00 40 01 7F+= 28-= F7
```

Specifying inverse commands makes it possible to move up the setlist within particular presets by reversing incremental changes. An example is going up to re-enable patch 1, which was disabled when going down. This makes navigation easier and more fail-safe. It also allows for a consistency check, where you get a warning message if you apply the same code twice in the same preset, without reversing it in between. This is important, because it would be inconsistent if you move up the list of settings. Note that sample wave and wait commands are ignored when going up.

## Finding your midi codes
The midi codes can be found quite easily in the application itself. When the device is properly connected, press CTRL-M to enable a display of all incoming midi on the screen. If you now play notes/send midi messages you want to use in the application, you will see the respective code/preset that can be used to send the same command. By selecting preset 1, you will see the hex code for preset one that the device needs. Copy the messages over (select and right-click to copy with the mouse, or use the log feature from the Display menu bar) and write one line in the midi configuration. 

## Making or modyfing a setlist
A setlist starts with a name on the first line for your own reference (see also the example below). Afterwards, lines can have to formats, one for presets and one for incremental changes. The format is comma-separated. When making a change to the setlist on disk, just save the file (and close it, if you're done), press CTRL-R in the application to reload the setlist.

Most important to note here is that either a new preset is called for, or an incremental change. New presets call a preset and define midi code labels. Incremental changes are made by executing midi codes. Song names and position in songs can be added in both cases for visual clues regarding the setlist content. The format is made such that it is extremely flexible and, after some experience, very fast to write.

A new preset is configured as follows: 
```
Song name,Position in song,Preset,Midi code labels (comma-separated list of code and readable labels)
```

An incremental change is configured as follows: 
```
Song name,Position in Song,,Midicodes (comma-separated)
```

Each element is now described in detail. Please see below for an example file, with examples of all the options. That will clarify how to write setlists.
* The *songname* needs only to be presented when it starts, the application copies it over until it changes.
* The *position in song* is for the user to easily indicate where this setlist item is activated, for instance a measure number or letter or a 'refrain'.
* The *preset* switches a preset that is self-contained, that is, you can switch from any position to another preset and no history of other codes matter. When there is a preset, no incremental changes are read. The rest of the line is reserved for listing the patch sounds that are configured in this preset. This enables the application to display the current sounds also when incremental changes are made later (displaying sound 1 when midicode +1 is executed for instance). 
* The *midi code labels* are a comma-separated list that gives labels to midi codes. These can de different for different songs. For instance the sound name on patch for a particular song can be the label for the midi code that enables that patch. Code labels are given in comma separated list of midi code,midi label, only for new presets. When an incremental change is made for which a code label is present, it will be displayed on the right in the dashboard. 
* The *midi codes* are a comma-separated list of incremental changes within the current preset. Each change is sent seperately to the midi device. When moving up and down the setlist within a preset, incremental changes (or their reverse) are executed, and the preset is not reset. Note that the Preset place-holder remains empty here, so there is a double comma.

Midi codes have different forms. The following types are implemented:
* Midi codes as defined in midi.txt. The codes, s1 and s2, for instance, match the s= line and add 1 and 2 respectively, loading presets 1 and 2. The incremental change shortcuts (+1 until +16) are midi codes configured for the midi device that follow the += command, which enable patch 1 until 16.

* In order to make changes reversible, from and to codes are separated with >
```
CommandFrom>CommandTo
```

* System exclusive hex messages can be directly used as command:
```
F0 41 10 00 00 77 12 18 00 40 02 01 25 F7
```

* Midi program change messages use the following format:
```
PC-Channel-Program
```

* Control Changes can be done as follows (Duration can be ignored. If duration is ignored, FromValue can also be ignored, but this prevents reversing the change. Duration is in miliseconds.
```
CC-Channel-Control-FromValue-ToValue-Duration
```

* Delays between commands are added with a wait command, with a duration in miliseconds.
```
WAIT-Duration
```

* Samples can be played with the WAV command. Only wav files are currently supported and they play to the default audio device.
```
WAV-mywavfile.wav
```

* Lines that start with a semicolon and empty lines are ignored (so the semicolon can be used for comments.

**Example setlist.txt:**
```
Example setlist

;For studiosets
;Song name,Place in song,Preset,Midi code labels (code comma label)
Preset 1,,s1
My favorite song,,s2,+1,Piano,+2,Strings,+3,Organ,+4,Flute

;Song name,Place in song,empty,Midicodes
,Verse 1,,+2,-1

;Enable parts
,Refrain,,+3,+4

;Disable parts
,Bridge,,-2

;Program changes
,Part 1 to grand piano,,PC-1-0
,Part 2 to bright piano,,PC-2-1
,Reversible program change part 3 from 0 to 1,,PC-3-0-1

;Control changes
,50,,CC-1-11-64
,51,,CC-1-11-30
,Reversible control change,,CC-1-11-47-111
,Reversible control change with duration,,CC-1-11-64-32-100
;Control change 11 (expression, 7 = volume), from 64 to 32 over 100 ms

;Reversible commands with >
,Part 1 grand piano to bright piano,,PC-2-0>PC-2-1
,From +1 to +2,,+1>+2
,From +3 to -4,,+3>-4

;select part 7
,74,,p7

;mute part 5
,75,,M5

;unmute part 2
,76,,m2

;Part 2: key range 50 till 70
,77,,l02:50,u02:70

;Change octaves
,Parts 1 and 2 octaves +1,,o01:5,o02:5
,Parts 3 and 4 octaves -2,,o01:2,o02:2

;Fancy stuff
,Play sample Wait 1 second and play again,,WAV-1.wav,WAIT-1000,WAV-1.wav
```
