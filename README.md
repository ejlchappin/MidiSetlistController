# Introduction
The MidiSetListController enables quick but sophisticated setlists with elements that sends particular midi messages to synthesizers or other midi devices, for live performances or studio usages, with intuitive navigation features. A key feature is that the setlist can easily shift between presets, but also by modifying the preset on the fly, for instance enable or disable layered sounds. The setlist can also be navigated by a midi pedal that sends a particular control change message.

The controller is developed in order to be very quick in making and modifying rather complicated setlists and maintain a stable setting for live performances. 

![screenshot](https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.png)

# Install
Save the application to disk: https://github.com/ejlchappin/MidiSetlistController/raw/master/MidiSetlistController.jar 

Save the setlist to disk: https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/setlist.ini

Save the midi configuration to disk: https://raw.githubusercontent.com/ejlchappin/MidiSetlistController/master/config.ini

The configuration (and setlist) needs to be adjusted for your own midi device (see below).

Start command-line/terminal and navigate to the folder. Start the application with:

java -jar MidiSetlistController.jar

# Using the MidiSetlistController
The applicaton opens with the menu, which can always be recalled by pressing M. If the config.ini and setlist.ini files are present in the same folder they are loaded. Otherwise use the key F to load a config file and G to load a setlist file. The application attempts to reach the configured midi device and will show you the result. 

When a setlist is loaded, you can navigate the setlist. With the arrow keys you move individual settings and page-up and page-down make you move between complete presets. The logic takes care of performing the correct incremental changes, for instance when moving up.

At the bottom of the screen, in large, the current item in the setlist is shown, for a clear view during live performances.

# Configuring midi device
The midi device is configured as follows. See the example below for an example (a Roland FA-08).

The first line selects the *midi device*. The midi device is configured by name, which can be found by pressing D, which lists all the devices available in the system. The name is put on the first line of the config.ini file.

The second line is reserved for the navigation *pedal command* which is configured to a control change message, i.e. the channel, control change and value. These three are put, comma-separated, in this order. By pressing the navigation pedal, the setlist progresses one step. This means, no interaction with the computer is necessary during live performance.

From the third line *midi message shortcuts* are configured, according to Shortcut,Midicode,InverseShortcut. First the shortcut, that is the shortcut used in your setlist, the System Exclusive Message formatted in 7bit bytes, and the inverse command shortcut to undo the change, if that is available. Here are two commands listed, the first enabling patch 1 and the second disabling patch 1. These are each others inverse commands, so the third element of +1 contains -1 and vice versa. 

*Example config.ini:*

FA-06 08

16,9,127

+1,240 65 16 0 0 119 18 24 0 64 2 1 37 247,-1

-1,240 65 16 0 0 119 18 24 0 64 2 0 38 247,+1

Specifying inverse commands makes it possible to move up the setlist within particular presets, by reversing the incremental changes that were done. For instance going up to re-enable patch 1, that was disabled when going down. This makes navigation easier and more fail-safe.

# Finding your midi codes
The midi code can be found quite easily in the application. Press I to enable the midi receiver printing it's output to the screen. If you now play notes/send midi messages you want with your device, you will see the result. Copy the correct system exclusive message over, and translate the Hex code to a 7bit decimal version in this webpage http://mididesigner.com/help/midi-byte-calculator/ (enter the code under Bytes to values, and find the right code in the individually listed decimal numbers under Result).

# Making or modyfing a setlist
A setlist starts with a name on the first line for your own reference (see also the example below). Afterwards, each line has the same format, seperated with commas: Songname,Measure,Preset,Incremental changes

The *songname* needs only to be presented when it starts, the application copies it over until it changes.

The *measure* is purely for the user, to indicate where this setlist item is activated.

The *preset* switches a preset that is self-contained, that is, you can switch from any position to another preset and no history of other codes matter. When there is a preset, no incremental changes are read. The rest of the line is reserved for listing the patch sounds that are configured in this preset. This enables the applicaiton to display the current sounds also when incremental changes are made later. In my example, 15 sounds can be configured the 16th channel is reserved for the navigation pedal.

The *incremental changes* are a comma-separated list of changes within the current preset. Each change is sent seperately to the midi device. When moving up and down the setlist within a preset, incremental changes (or their reverse) are executed, and the preset is not reset.

This implies that the preset shortcuts (in the example below 1, 12, 17), and the incremental change shortcuts (all the + and - codes) are midi codes configured for the midi device in config.ini. If codes are not configured, for instance because of a typo in the setlist, an error message is shown.

*Example setlist.ini:*

Prestige 2016

Basic,,1,

Song of Purple Summer,,12,Strs,SmallStrs,SmallStrs R,SmallStrss R 8ve lower,Fullstrs R 8ve lower,Harp L

,16,,+3,+4,+6,-2

,20,,+1,-3,-4,-6

,35,,+5

,43,,-5

Bohemienne,,17,Strs,,SftStrgs,,Warm pd L,Warm pd,Harpsichord,Harp,Folk harp,,Dulcimer,,Glock R soft,Glock R,Silent strs R

,37,,+9,-5,-14,-15

# TODO and bugs

Start the application with a commandline (terminal, from a batch file), otherwise the DumpReceiver doesn't work. 

The conversion from Hex midi codes to 7bit decimal bytes should be done in the application and allows to enter hex midi codes too.


