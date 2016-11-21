/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package midisetlistcontroller;

import java.awt.event.KeyEvent;
import javax.sound.midi.ShortMessage;

/**
 *
 * @author ejlchappin
 */
public class MidiTrigger {

    int keyEvent;
    String key;
    int messageType;
    int channel;
    int control;
    int value;
    String midiCode;
    String sysexHex; 
    String messageTypeString;
    public static final int SystemExclusive = 0;
    public static final int MidiCode = 1;

    public MidiTrigger(String key, String messageTypeString, int channel, int control, int value, String sysexMessage, String midiCode) {

        this.channel = channel;
        this.control = control;
        this.value = value;
        this.sysexHex = sysexMessage;
        this.midiCode = midiCode;
        
        this.key = key;
        switch (key) {
            case "Down":
                this.keyEvent = KeyEvent.VK_DOWN;
                break;
            case "Up":
                this.keyEvent = KeyEvent.VK_UP;
                break;
            case "Left":
                this.keyEvent = KeyEvent.VK_LEFT;
                break;
            case "Right":
                this.keyEvent = KeyEvent.VK_RIGHT;
                break;
            case "Page up":
                this.keyEvent = KeyEvent.VK_PAGE_UP;
                break;
            case "Page down":
                this.keyEvent = KeyEvent.VK_PAGE_DOWN;
                break;
            case "Home":
                this.keyEvent = KeyEvent.VK_HOME;
                break;
            case "End":
                this.keyEvent = KeyEvent.VK_END;
                break;
        }
        switch (messageTypeString) {
            case "CC":
                this.messageType = ShortMessage.CONTROL_CHANGE;
                break;
            case "PC":
                this.messageType = ShortMessage.PROGRAM_CHANGE;
                break;
            case "SE":
                this.messageType = MidiTrigger.SystemExclusive;
                break;
            case "MC":
                this.messageType = MidiTrigger.MidiCode;
                break;
        }
        this.messageTypeString = messageTypeString;
    }
    
    @Override
    public String toString(){
         return "Midi trigger key: " + key + " to type: " + messageTypeString + ", channel: " + channel + ", control: " + control + ", value: " + value + ", midicode: " + midiCode + ", hexcode: " + sysexHex;
    }
}
