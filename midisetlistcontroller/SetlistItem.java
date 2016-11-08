package midisetlistcontroller;

import java.util.Arrays;

/**
 *
 * @author ejlchappin
 */
public class SetlistItem {

    private String songName;
    private int Measure;
    private int studioset;
    private String[] midiCodes;
    private String[] sounds;

    SetlistItem(String[] code, String mostRecentSong, String[] mostRecentSounds) {
        this.setSongName(code[0]);
        if (code.length > 1) {
            if (!code[1].equals("")) {
                this.setMeasure(Integer.valueOf(code[1]));
            }
        }
        if (code.length > 2) {
            if (!code[2].equals("")) {
                this.setStudioset(Integer.valueOf(code[2]));
            }
        }

        if (code[0].equals("")) {
            //patch within song, so the elements from nr 4 list enabling/disabling parts.
            if (code.length > 3) {
                this.setMidiCodes(Arrays.copyOfRange(code, 3, code.length));
            }
            //We copy over the most recents song name and sounds.
            this.setSounds(mostRecentSounds);
            this.setSongName(mostRecentSong);
        } else {
            //new song, so the elements from nr 4 list the sounds of this patch
            if (code.length > 3) {
                this.setSounds(Arrays.copyOfRange(code, 3, code.length));
            }
        }
    }

    public int getStudioset() {
        return studioset;
    }

    public void setStudioset(int studioset) {
        this.studioset = studioset;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public int getMeasure() {
        return Measure;
    }

    public void setMeasure(int Measure) {
        this.Measure = Measure;
    }

    public String[] getMidiCodes() {
        return midiCodes;
    }

    public void setMidiCodes(String[] midiCodes) {
        this.midiCodes = midiCodes;
    }

    public void SetlistItem(String[] name) {
    }

    public String[] getSounds() {
        return sounds;
    }

    public void setSounds(String[] sounds) {
        this.sounds = sounds;
    }

    public String getMidiCodesWritten() {
        if (midiCodes != null) {
            String s = Arrays.toString(midiCodes);
            return "\t" + s.substring(1, s.length() - 1).replaceAll(",", "");
        }
        return "";
    }

    //gives all the sound names of patches that are added 
    public String getSoundNamesAdded() {
        String sounds = "";
        if (getMidiCodes() != null) {
            for (String code : getMidiCodes()) {
                if (code.substring(0, 1).equals("+")) {
                    int patchChanged = Integer.parseInt(code.substring(1)) - 1;
                    if (getSounds() != null) {
                        if(getSounds().length > patchChanged){
                            sounds = sounds.concat("  " + getSounds()[patchChanged]);
                        }
                    }
                }
            }
        }
        if (sounds != ""){
            return "\t" + sounds;
        }
        return "";
    }

    public String getStudiosetWritten() {
        if (getStudioset() > 0) {
            return "\tSet: " + String.valueOf(getStudioset());
        }
        return "";
    }

    public String getMeasureWritten() {
        if (getMeasure() > 0) {
            return "\tM: " + String.valueOf(getMeasure());
        }
        return "";
    }

    public String toString() {
        return getSongName() + getMeasureWritten() + getStudiosetWritten() + getMidiCodesWritten() + getSoundNamesAdded();
    }
}
