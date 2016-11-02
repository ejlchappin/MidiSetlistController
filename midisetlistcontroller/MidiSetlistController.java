package midisetlistcontroller;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import static midisetlistcontroller.MidiSetlistController.newline;

/**
 * The MidiSetListController, enables quick but sophisticated setlists that
 * sends particular midi messages to synthesizers or other midi devices, for
 * live performances or studio usages.
 *
 * The application configures midi system exclusive messages to shortcut
 * commands. Setlists contain those shortcuts to quickly and intuitively build
 * and easily maintain setlists.
 *
 * User can navigate setlists with incoming midi messages from midi devices,
 * such as a pedal, by making use of a slightly modified DumpReceiver from
 * http://www.jsresources.org/examples/DumpReceiver.java.html
 *
 * To configuration files are needed: one for the device that is controlled and
 * another for the setlist. See the examples for details and how the
 * configuration files are formatted.
 *
 * @author Emile Chappin Contact: emile@chappin.com Published under the Apache
 * License 2.0
 */
public class MidiSetlistController extends JFrame
        implements KeyListener,
        ActionListener {

    MidiSetlistController frame = null;
    MidiDevice device = null;
    MidiDevice deviceToListen = null;
    HashMap<String, String> midiCodesMap = new HashMap<String, String>();
    HashMap<String, String> midiCodesOpposits = new HashMap<String, String>();
    ArrayList<SetlistItem> setlist = new ArrayList<SetlistItem>();
    JTextArea displayArea;
    JTextArea displayAreaCurrent;
    final JFileChooser fc = new JFileChooser();
    static final String newline = System.getProperty("line.separator");
    int currentSetListIndex = -1;
    int numberFromKeys = 0;
    String configFilename = System.getProperty("user.dir") + "/config.ini";//default filename
    String setlistFilename = System.getProperty("user.dir") + "/setlist.ini";//default filename
    int pedalChannel;
    int pedalControlChange;
    int pedalValue;
    boolean printMidiReceived = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        //Use an appropriate Look and Feel
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        // Turn off metal's use of bold fonts //
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        //Schedule a job for event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public MidiSetlistController(String name) {
        super(name);
    }

    public MidiSetlistController(String name, MidiSetlistController myframe) {
        super(name);
        this.frame = myframe;
    }

    /**
     * Create the GUI and show it.
     */
    private static void createAndShowGUI() {

        //Create and set up the window.
        MidiSetlistController frame = new MidiSetlistController("Midi programma");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        //Set up the content pane.
        frame.addComponentsToPane();

        //Display the window.
        frame.pack();
        frame.setVisible(true);

        MidiListener thread = new MidiListener(frame);
        thread.setDaemon(true);
        thread.start();

    }

    // Add stuff and listeners to the GUI
    private void addComponentsToPane() {

        //Add button
        JButton button = new JButton("Load config and setlist");
        button.addActionListener(this); //listen to a button click.
        button.addKeyListener(this); //even if the button is selected, keys can be pressed.

        //Add display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new java.awt.Font("Arial", 0, 16));
        //displayArea.setTabSize(8);
        displayArea.setLineWrap(true);
        displayArea.addKeyListener(this); //if keys are pressed when the display area is in focus.
        JScrollPane scrollPane = new JScrollPane(displayArea);

        //Add display area
        displayAreaCurrent = new JTextArea();
        displayAreaCurrent.setEditable(false);
        displayAreaCurrent.setFont(new java.awt.Font("Arial", 0, 38));
        displayAreaCurrent.setLineWrap(false);
        displayAreaCurrent.setTabSize(6);
        displayAreaCurrent.addKeyListener(this); //if keys are pressed when the display area is in focus.
        JScrollPane scrollPaneCurrent = new JScrollPane(displayAreaCurrent);
        scrollPaneCurrent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        //Place button and text area on the screen
        getContentPane().add(button, BorderLayout.PAGE_START);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(scrollPaneCurrent, BorderLayout.PAGE_END);

        //Write the menu options to the text area
        writeMenu();
        readMidiConfig();
        openMidiDevice();
        readSetlist();
        displayArea.requestFocusInWindow();
    }

    // When button is clicked
    public void actionPerformed(ActionEvent e) {

        //Read and process configuration.
        readMidiConfig();
        openMidiDevice();
        readSetlist();

        //Return the focus to the typing area.
        displayAreaCurrent.requestFocusInWindow();
    }

    // When key is pressed
    private void handleKey(KeyEvent e) {

        //Find out which key is pressed
        int id = e.getID();
        String key = null;
        if (id == KeyEvent.KEY_TYPED) {
            Character c = e.getKeyChar();
            key = c.toString();
        } else {
            int keyCode = e.getKeyCode();
            key = KeyEvent.getKeyText(keyCode);
        }

        //Translate different keys to differen actions:
        if (key.equals("A")) {
            readMidiConfig();
            openMidiDevice();
        } else if (key.equals("C")) {
            closeMidiDevice();
        } else if (key.equals("D")) {
            listMidiDevices();
        } else if (key.equals("M")) {
            writeMenu();
        } else if (key.equals("O")) {
            openMidiDevice();
        } else if (key.equals("S")) {
            readSetlist();
        } else if (key.equals("L")) {
            displaySetlist();
        } else if (key.equals("U")) {
            displayMidiCodes();
        } else if (key.equals("Y")) {
            displayMidiCodesAvailable();
        } else if (key.equals("T")) {
            testSendMessage();
        } else if (key.equals("I")) {
            togglePrintMidiReceived();
        } else if (key.equals("X")) {
            clearTextArea();
        } else if (key.equals("F")) {
            if (chooseConfigFile()) {
                readMidiConfig();
            }
        } else if (key.equals("G")) {
            if (chooseSetlistFile()) {
                readSetlist();
            }
        } else if (key.equals("Up")) {
            runSetlistPrevious();
        } else if (key.equals("Left")) {
            runSetlistPrevious();
        } else if (key.equals("Down")) {
            runSetlistNext();
        } else if (key.equals("Right")) {
            runSetlistNext();
        } else if (key.equals("Space")) {
            runSetlistNext();
        } else if (isNumeric(key)) {
            // write number to the screen (and keep it)
            numberFromKeys = 10 * numberFromKeys + intValue(key);
            write(key);
        } else if (key.equals("Home")) {
            runSetlistStart();
        } else if (key.equals("End")) {
            runSetlistEnd();
        } else if (key.equals("Page Up")) {
            runSetListPreviousPreset();
        } else if (key.equals("Page Down")) {
            runSetListNextPreset();
        } else if (key.equals("Enter")) {
            runSetlistItemFromKeyboard();
        }
        displayAreaCurrent.requestFocusInWindow();
    }

    ///////NAVIGATION////////////////
    // Goes to the beginning of the setlist
    public void runSetlistStart() {
        runSetlistItemNumber(0);
    }

    // Goes to the end of the setlist
    public void runSetlistEnd() {
        runSetlistItemNumber(setlist.size() - 1);
    }

    // Goes to the the next setlist item
    public void runSetlistNext() {
        if (currentSetListIndex < setlist.size() - 1) {
            runSetlistItemNumber(currentSetListIndex + 1);
        }
    }

    // Goes to the previous setlist item 
    public void runSetlistPrevious() {
        if (currentSetListIndex >= 1) {
            runSetlistItemNumber(currentSetListIndex - 1);
        }
    }

    // Goes to the beginning of this preset (or the previous preset)
    public void runSetListPreviousPreset() {
        boolean previousPresetFound = false;
        int previousPreset = currentSetListIndex;
        while (!previousPresetFound && previousPreset > 0) {
            previousPreset--;
            if (setlist.get(previousPreset).getStudioset() != 0) {
                //found next preset
                previousPresetFound = true;
            }
        }
        runSetlistItemNumber(previousPreset);
    }

    // Goes to the next preset
    public void runSetListNextPreset() {
        boolean nextPresetFound = false;
        int nextPreset = currentSetListIndex;
        while (!nextPresetFound & nextPreset < setlist.size() - 1) {
            nextPreset++;

            if (setlist.get(nextPreset).getStudioset() != 0) {
                //found next preset
                nextPresetFound = true;
                runSetlistItemNumber(nextPreset);
            }
        }
    }

    // Goes to the setlist item entered on the keyboard or the current one if nothing (valid) was entered.
    public void runSetlistItemFromKeyboard() {
        if (numberFromKeys > 0 && numberFromKeys < setlist.size()) {
            //Run the setlist item entered on the keyboard
            writeLine("");
            runSetlistItemNumber(numberFromKeys);
        } else {
            //Run the current selection
            runSetlistItemNumber(currentSetListIndex);
        }
        numberFromKeys = 0;
    }

    // Determine the strategy to go from the current set list index to a particular position and execute that strategy.
    public void runSetlistItemNumber(int goingTo) {

        //Special case: when we start up, we start at -1.
        if (currentSetListIndex == -1) {
            currentSetListIndex = 0;
        }

        int comingFrom = currentSetListIndex;

        if (goingTo == comingFrom) {

            //Case 1. We are not moving, just reapplying the current setting.
            runCurrentSetlistItem(false);
        } else if (goingTo > comingFrom) {

            //Case 2. We're moving downwards.
            //Is there a later preset we can start from?
            int latestPresetFound = comingFrom + 1;
            for (int i = latestPresetFound; i <= goingTo; i++) {
                if (setlist.get(i).getStudioset() != 0) {
                    //found later preset at nr i.
                    latestPresetFound = i;
                }
            }

            //Proceed to where we want to go
            for (int i = latestPresetFound; i <= goingTo; i++) {
                currentSetListIndex = i;
                runCurrentSetlistItem(false);
            }

        } else if (comingFrom > goingTo) {

            //Case 3. We're moving upwards.
            boolean differentPreset = false;

            // Is there a preset change in the meantime (note that goingTo may have a preset, but that means there is NO preset change
            for (int i = comingFrom; i > goingTo; i--) {
                if (setlist.get(i).getStudioset() != 0) {
                    differentPreset = true;
                }
            }

            if (!differentPreset) {

                // Case 3a. We are moving up withing the same preset, which means we reversely apply the changes upwards
                for (int i = comingFrom; i > goingTo; i--) {
                    // run the reverse of each one, except for the target.
                    currentSetListIndex = i;
                    runCurrentSetlistItem(true);
                }
                currentSetListIndex = goingTo;

            } else {

                // Case 4b. We are moving to a different preset, which means we should go to the beginning of the preset and go down to where we need to go.
                boolean presetFound = false;
                int previousPreset = goingTo;
                while (!presetFound && previousPreset > 0) {
                    previousPreset--;
                    if (setlist.get(previousPreset).getStudioset() != 0) {
                        presetFound = true;
                    }
                }

                //We need to go from presetFound up to goingTo.
                for (int i = previousPreset; i <= goingTo; i++) {
                    currentSetListIndex = i;
                    runCurrentSetlistItem(false);
                }
            }
        }
        // Add outcome to the display
        writeLine(currentSetListIndex + ": " + setlist.get(currentSetListIndex).toString());
    }

    //Run current set list item
    public void runCurrentSetlistItem(boolean reverseDirection) {

        System.out.println("Loading preset " + currentSetListIndex + ", reverse codes: " + reverseDirection);
        SetlistItem item = setlist.get(currentSetListIndex);
        String[] midiCodes = item.getMidiCodes();
        Integer studioset = item.getStudioset();

        // Normal direction
        if (!reverseDirection) {

            // Send a studio set message if it is needed
            if (studioset > 0) {
                if (midiCodesMap.containsKey(studioset.toString())) {
                    sendMessage(midiCodesMap.get(studioset.toString()));
                } else {
                    writeLine("Error 6a, midicodes map doesn't contain studioset " + studioset);
                }
            }

            // Send other midi codes
            if (midiCodes != null) {
                for (String s : midiCodes) {
                    if (midiCodesMap.containsKey(s)) {
                        sendMessage(midiCodesMap.get(s));
                    } else {
                        writeLine("Error 6b, midicodes map doesn't contain midi code for message " + s);
                    }
                }
            }

        } else {

            // Reverse direction, so we reverse the codes!

            // Do we have codes?
            if (midiCodes != null) {

                // For each of the codes
                for (String s : item.getMidiCodes()) {

                    // Do we have a reverse code?
                    if (midiCodesOpposits.containsKey(s)) {

                        // Send the reverse code
                        String reverseMidiCode = midiCodesOpposits.get(s);
                        if (midiCodesMap.containsKey(reverseMidiCode)) {
                            sendMessage(midiCodesMap.get(reverseMidiCode));
                        } else {
                            writeLine("Error 6b, midicodes map doesn't contain midi code for message " + reverseMidiCode);
                        }

                    } else {

                        // The reverse is not defined, so we send the original
                        if (midiCodesMap.containsKey(s)) {
                            sendMessage(midiCodesMap.get(s));
                        } else {
                            writeLine("Error 6b, midicodes map doesn't contain midi code for message " + s);
                        }
                    }
                }
            }
        }
    }

    //Writes the menu options to the text area
    public void writeMenu() {
        writeLine("Config file: " + configFilename
                + newline + "Setlist file: " + setlistFilename
                + newline
                + newline + "Navigation:\tUse arrow keys to navigate (up/left for the previous item, right/down/space or pedal for the next item), or type number and press enter for a particular item"
                + newline + "\tUse Page up for previous preset, Page down for next preset, and press pedal for next item"
                + newline + "Other options:\tF: Choose config file\tA: Re-apply config\tY: Display midicodes\tU: Display midicodes in detail"
                + newline + "\tG: Choose setlist file\tS: Reload setlist\tL: Display setlist"
                + newline + "\tD: List midi devices\tO: Open midi device\tC: Close midi device\tT: Test midi sound"
                + newline + "\tM: Display this menu\tX: Clear screen\tI: Toggle display midi input\tButton: reload config and setlist"
                + newline);
    }

    private boolean chooseConfigFile() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            configFilename = file.getPath();
            return true;
        }
        return false;
    }

    //Reads configuration file for midi device and codes
    public void readMidiConfig() {
        BufferedReader reader = readFile(configFilename);
        String name = "";

        //First line is midi device name
        try {
            name = reader.readLine();
        } catch (Exception e) {
            writeLine("Error 5a: file read error");
        }
        writeLine("Midi device configured: " + name);
        selectMidiDevice(name);

        //Second line is pedal information: channel, control change, value
        try {
            name = reader.readLine();
        } catch (Exception e) {
            writeLine("Error 5a: file read error");
        }
        String[] secondLine = name.split(",");
        if (secondLine.length == 3) {
            pedalChannel = intValue(secondLine[0]);
            pedalControlChange = intValue(secondLine[1]);
            pedalValue = intValue(secondLine[2]);
            writeLine("Midi pedal configuration set to channel: " + pedalChannel + ", control change: " + pedalControlChange + ", value: " + pedalValue);
        } else {
            writeLine("Midi pedal configuration not correct on second line of config.");
        }

        //Read midi codes.
        try {
            while (reader.ready()) {
                name = reader.readLine();
                String[] code = name.split(",");
                if (code.length >= 2) {
                    midiCodesMap.put(code[0], code[1]);
                } else {
                    writeLine("Malformed midi code: " + code);
                }
                if (code.length >= 3) {
                    midiCodesOpposits.put(code[0], code[2]);
                }
            }
        } catch (Exception e) {
            writeLine("Error 5b: file read error");
        }
    }

    //Write out the midi codes 
    public void displayMidiCodes() {
        writeLine("Midi codes configured:");
        for (Map.Entry<String, String> entry : midiCodesMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String opposite = midiCodesOpposits.get(key);
            if (opposite != null) {
                writeLine("Key: " + key + " -- value: " + value + " and opposite: " + opposite);
            } else {
                writeLine("Key: " + key + " -- value: " + value + ", no opposite");
            }
        }
    }

    //Write out the midi codes 
    public void displayMidiCodesAvailable() {
        write("Midi codes configured: ");
        for (String s : midiCodesMap.keySet()) {
            write(s + ", ");
        }
        writeLine("");
        write("Opposites configured for: ");
        for (String s : midiCodesOpposits.keySet()) {
            write(s + "(" + midiCodesOpposits.get(s) + "), ");
        }
        writeLine("");
    }

    //Chooses a setlist file
    private boolean chooseSetlistFile() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            setlistFilename = file.getPath();
            return true;
        }
        return false;
    }

    //Read setlist from the selected file
    public void readSetlist() {
        BufferedReader reader = readFile(setlistFilename);
        String name = null;
        try {
            name = reader.readLine();
        } catch (Exception e) {
            writeLine("Error 5a: file read error");
        }
        writeLine(newline + "Setlist: " + name);

        setlist = new ArrayList<>();
        String mostRecentSongName = "";
        String[] mostRecentSounds = null;
        try {
            while (reader.ready()) {
                name = reader.readLine();
                String[] code = name.split(",");
                SetlistItem item = new SetlistItem(code, mostRecentSongName, mostRecentSounds);
                mostRecentSongName = item.getSongName();
                mostRecentSounds = item.getSounds();
                setlist.add(item);
            }
        } catch (Exception e) {
            writeLine("Error 5b: file read error");
        }
    }

    //Writes out the setlist
    public void displaySetlist() {
        writeLine("Set list items:");
        for (SetlistItem item : setlist) {
            writeLine(item.toString());
        }
    }

    //Finds the midi device in the system with a name 
    public void selectMidiDevice(String name) {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        boolean found = false;
        for (int i = 0; i < infos.length; i++) {
            try {
                if (infos[i].getName().equals(name)) {
                    MidiDevice thisDevice = MidiSystem.getMidiDevice(infos[i]);
                    int r = thisDevice.getMaxReceivers();
                    int t = thisDevice.getMaxTransmitters();
                    writeLine("Found the midi device you configured, with " + r + " receivers and " + t + " transmitters");
                    if (t == 0) {
                        device = thisDevice;
                        found = true;
                    }
                    if (r == 0) {
                        deviceToListen = thisDevice;
                    }

                }
            } catch (MidiUnavailableException e) {
                writeLine("Error 5, could not load device.");
            }
        }
        if (found == false) {
            writeLine("Didn't find the device");
        }
    }

    //Writes a list of the midi devices in the system
    public void listMidiDevices() {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            Info info = infos[i];
            writeLine("Device name:" + info.getName() + ", vendor:" + info.getVendor() + ", version:" + info.getVersion() + ", description:" + info.getDescription());
        }
    }

    //Opens the currently selected midi device.
    public void openMidiDevice() {
        writeLine("Opening configured midi device");
        if (device == null) {
            writeLine("No device selected yet");
        } else if (!(device.isOpen())) {
            try {
                device.open();
                writeLine("Device is now opened");
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
                writeLine("Error 3, opening device exclusively.");
            }
        } else {
            writeLine("Device was already opened");
        }
    }

    //Closes the currently selected midi device.
    public void closeMidiDevice() {
        writeLine("Closing currently selected device");
        if (device == null) {
            writeLine("No device selected yet");
        } else if (device.isOpen()) {
            try {
                device.close();
                writeLine("Device is now closed");
            } catch (Exception e) {
                writeLine("Error 4, closing device");
            }
        } else {
            writeLine("Device was already closed");
        }
    }

    //Sends a test note to the midi device
    public void testSendMessage() {
        ShortMessage myMsg = new ShortMessage();

        try {
            // Start playing the note Middle C (60), 
            // moderately loud (velocity = 93).
            myMsg.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);
        } catch (InvalidMidiDataException ex) {
            writeLine("Error 1: bad midi message");
        }
        long timeStamp = -1;
        try {
            Receiver rcvr = device.getReceiver();
            writeLine("Sending test tone");
            rcvr.send(myMsg, timeStamp);
        } catch (Exception e) {
            writeLine("Error 2b: no midi device selected");
        }
    }

    // Disables or enalbes writing the midi messages received to the screen
    public void togglePrintMidiReceived() {
        printMidiReceived = !printMidiReceived;
    }

    //Cleares the display area
    private void clearTextArea() {
        displayArea.setText("");
    }

    public void sendMessage(String messageBody) {
        SysexMessage sysmsg = new SysexMessage();
        try {
            byte[] messageData = byteStringToByteArray(messageBody);
            sysmsg.setMessage(messageData, messageData.length);

        } catch (Exception ex) {
            writeLine("Error 6c: bad midi message");
        }
        long timeStamp = -1;
        try {
            Receiver rcvr = device.getReceiver();
            rcvr.send(sysmsg, timeStamp);
        } catch (Exception e) {
            //writeLine("Error 6e: no midi device selected"); don't display this error...
        }
    }

    public static byte[] byteStringToByteArray(String s) {
        String[] stringArray = s.split(" ");
        int len = stringArray.length;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            int j = Integer.parseInt(stringArray[i]);
            data[i] = (byte) j;
        }
        return data;
    }

    //Reads the file with filename and gives the reader back
    public BufferedReader readFile(String filename) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filename));
        } catch (Exception e) {
            writeLine("Error 4: file open error");
        }
        return br;
    }

    public void writeLine(String line) {
        displayArea.append(line + newline);
        displayArea.setCaretPosition(displayArea.getDocument().getLength());

        if (setlist != null) {

            // update the current item in the list at the bottom of the screen
            if (setlist.size() > currentSetListIndex && currentSetListIndex >= 0) {
                displayAreaCurrent.setText(currentSetListIndex + ": " + setlist.get(currentSetListIndex).toString());
            }
        }
    }

    public void write(String line) {
        displayArea.append(line);
        displayArea.setCaretPosition(displayArea.getDocument().getLength());
    }

    //When a key is pressed...
    @Override
    public void keyPressed(KeyEvent e) {
        handleKey(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void pedalTrigger() {
        runSetlistNext();
        displayArea.setCaretPosition(displayArea.getDocument().getLength());
        displayAreaCurrent.requestFocusInWindow();
    }

    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static int intValue(String str) {
        int d = 0;
        try {
            d = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
        return d;
    }

    // Configure the midi receiver
    public void listenToDevice() {
        while (displayArea == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Skip if the deviceToListen is not set yet.
        if (deviceToListen != null) {
            writeLine("Midi device to listen to is set");
            try {
                deviceToListen.open();
                writeLine("Midi device to listen to is opened");
            } catch (MidiUnavailableException ex) {
                writeLine("Midi device to listen to couldn't be opened");
            }
            Receiver r = new DumpReceiver(System.out, this);
            try {
                Transmitter t = deviceToListen.getTransmitter();
                t.setReceiver(r);
            } catch (MidiUnavailableException e) {
                writeLine("Error receiver 1");
            }

            try {
                Transmitter t = deviceToListen.getTransmitter();
                t.setReceiver(r);
            } catch (MidiUnavailableException e) {
                writeLine("Error receiver 2");
            }

            try {
                //TODO this doesn't work without a command line, somehow. Can we make it work when the jar file just runs?
                System.in.read();
            } catch (IOException ioe) {
                writeLine("Error receiver 3");
            }
            deviceToListen.close();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public static class MidiListener extends Thread {

        MidiSetlistController midicontroller = null;

        public MidiListener(MidiSetlistController m) {
            this.midicontroller = m;
        }

        @Override
        public void run() {
            while (true) {
                midicontroller.listenToDevice();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
