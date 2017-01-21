package midisetlistcontroller;

import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author ejlchappin
 */
public class SetlistItem {

    private String songName;
    private String position;
    private int preset;
    private String[] midiCodes;
    private HashMap<String, String> midiCodeLabels;

    SetlistItem(String[] code, String mostRecentSong, HashMap<String, String> mostRecentMidiCodeLabels) {
        this.midiCodeLabels = new HashMap<>();

        this.setSongName(code[0]);
        if (code.length > 1) {
            this.setPosition(code[1]);
        }
        if (code.length > 2) {
            if (!code[2].equals("")) {
                this.setPreset(Integer.valueOf(code[2]));
            }
        }

        if (code[0].equals("")) {
            //patch within song, so the elements from nr 4 list enabling/disabling parts.
            if (code.length > 3) {
                this.setMidiCodes(Arrays.copyOfRange(code, 3, code.length));
            }
            //We copy over the most recents song name and sounds.
            this.setSongName(mostRecentSong);
            midiCodeLabels.putAll(mostRecentMidiCodeLabels);
        } else {
            //new song, so the elements from nr 4 list the sounds of this patch
            if (code.length > 3) {
                for (int i = 3; i < code.length - 1; i += 2) {
                    this.midiCodeLabels.put(code[i], code[i + 1]);
                }
            }
        }
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String[] getMidiCodes() {
        return midiCodes;
    }

    public void setMidiCodes(String[] midiCodes) {
        this.midiCodes = midiCodes;
    }

    public void SetlistItem(String[] name) {
    }

    public HashMap<String, String> getMidiCodeLabels() {
        return midiCodeLabels;
    }

    public void setMidiCodeLabels(HashMap<String, String> midiCodeLabels) {
        this.midiCodeLabels = midiCodeLabels;
    }

    public String getMidiCodesWritten() {
        if (midiCodes != null) {
            String s = Arrays.toString(midiCodes);
            return "\t" + s.substring(1, s.length() - 1).replaceAll(",", "");
        }
        return "";
    }

    //gives all the sound names of patches that are added 
    public String getMidiCodeLabelsWriten() {
        String labels = "";
        if (midiCodeLabels != null && getMidiCodes() != null) {
            for (String code : getMidiCodes()) {
                String codeLabel = midiCodeLabels.get(code);
                if (codeLabel != null) {
                    labels = labels.concat("  " + codeLabel);
                }
            }
        }
        if (labels != "") {
            return "\t" + labels;
        }
        return "";
    }

    public String getPresetWritten() {
        if (getPreset() > 0) {
            return "\tSet: " + String.valueOf(getPreset());
        }
        return "";
    }

    public String getMeasureWritten() {
        if (!getPosition().equals("")) {
            return "\t@ " + getPosition();
        }
        return "";
    }

    public String toString() {
        return getSongName() + getMeasureWritten() + getPresetWritten() + getMidiCodesWritten() + getMidiCodeLabelsWriten();
    }
}
