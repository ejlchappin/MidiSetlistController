package midisetlistcontroller;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import static midisetlistcontroller.MidiSetlistController.newline;

/**
 * The MidiSetListController enables quick but sophisticated setlists with
 * elements that sends particular midi messages to synthesizers or other midi
 * devices, for live performances or studio usages, with intuitive navigation
 * features
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
        implements
        ActionListener {

    MidiSetlistController frame = null;
    MidiDevice device = null;
    MidiDevice deviceToListen = null;
    MidiListener listenerThread = null;
    boolean listenerShouldBeReopened = false;
    Receiver receiver = null;
    boolean checkMidiDeviceAvailable = true;
    HashMap<String, String> midiCodesMap = new HashMap<>();
    HashMap<String, String> midiCodesOpposits = new HashMap<>();
    ArrayList<MidiTrigger> triggerlist = new ArrayList<>();
    ArrayList<SetlistItem> setlist = new ArrayList<>();
    JTextArea displayArea;
    JTextArea displayAreaCurrent;
    final JFileChooser fc = new JFileChooser();
    static final String newline = System.getProperty("line.separator");
    int currentSetListIndex = 0;
    //String configFilename = System.getProperty("user.dir") + "/midi.txt";//default filename
    //String setlistFilename = System.getProperty("user.dir") + "/setlist.txt";//default filename
    String configFilename = new File("").getAbsoluteFile().getAbsolutePath() + "/midi.txt";
    String setlistFilename = new File("").getAbsoluteFile().getAbsolutePath() + "/setlist.txt";
    boolean printMidiReceived = false;
    int largeFontSize = 32;
    int smallFontSize = 16;
    public String computedPlaceHolder = "=";
    SamplePlayer samplePlayer = new SamplePlayer();
    boolean isWaiting = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final String[] arguments = args;
        //Use an appropriate Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }

        //Schedule a job for event dispatch thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI(arguments);
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
    private static void createAndShowGUI(String[] args) {

        //Create and set up the window.
        MidiSetlistController frame = new MidiSetlistController("Midi Setlist Controller");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
     

        if (args.length >= 1) {
            frame.configFilename = args[0];
        }
        if (args.length >= 2) {
            frame.setlistFilename = args[1];
        }

        //Set up the content pane.
        frame.addComponentsToPane();
        URL imageURL = frame.getClass().getClassLoader().getResource("midi.gif");
        Image image = Toolkit.getDefaultToolkit().getImage(imageURL);
        if(imageURL != null){
            frame.setIconImage(image);
        }
        
        //Display the window.
        frame.pack();
        frame.setVisible(true);

        //Start the midi listener thread
        frame.createListenerThread();
    }

    public void createListenerThread() {
        boolean existing = false;
        if (listenerThread != null) {
            if (listenerThread.isAlive()) {
                existing = true;
                writeLine("Not creating a listener, it exists");
            }
        }
        if (!existing) {
            MidiListener thread = new MidiListener(frame);
            thread.midicontroller = this;
            this.listenerThread = thread;
            thread.setDaemon(true);
            thread.start();
        }
    }

    // Add stuff and listeners to the GUI
    private void addComponentsToPane() {

        //Create the menu bar.
        JMenuBar menuBar = new JMenuBar();

        //Build the menu.
        JMenu menuMidi = new JMenu("Midi");
        menuMidi.setMnemonic(KeyEvent.VK_M);
        menuBar.add(menuMidi);

        JMenu menuSetlist = new JMenu("Setlist");
        menuSetlist.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menuSetlist);

        JMenu menuDisplay = new JMenu("Display");
        menuDisplay.setMnemonic(KeyEvent.VK_D);
        menuBar.add(menuDisplay);

        //Midi menu items
        JMenuItem menuItem = new JMenuItem("Select midi config file", KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("Edit midi config", KeyEvent.VK_E);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("Reload midi config", KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuMidi.addSeparator();

        menuItem = new JMenuItem("Select midi device", KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("List midi devices", KeyEvent.VK_D);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("Open midi device", KeyEvent.VK_O);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("Close midi device", KeyEvent.VK_C);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuMidi.addSeparator();

        menuItem = new JMenuItem("List midi shortcuts available", KeyEvent.VK_L);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("List midi shortcuts with codes", KeyEvent.VK_H);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuMidi.addSeparator();

        menuItem = new JMenuItem("Send midi test note", KeyEvent.VK_T);
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuItem = new JMenuItem("Toggle midi check while sending", KeyEvent.VK_M);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        menuMidi.addSeparator();

        menuItem = new JMenuItem("Send midi code", KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuMidi.add(menuItem);

        //Setlist menu items
        menuItem = new JMenuItem("Select setlist file", KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        menuItem = new JMenuItem("Reload setlist", KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        menuItem = new JMenuItem("Edit setlist", KeyEvent.VK_E);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        menuSetlist.addSeparator();

        menuItem = new JMenuItem("Display setlist", KeyEvent.VK_D);
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        menuItem = new JMenuItem("Check setlist", KeyEvent.VK_C);
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        menuItem = new JMenuItem("Go to item...", KeyEvent.VK_G);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuSetlist.add(menuItem);

        //Display menu items
        menuItem = new JMenuItem("Toggle incoming midi", KeyEvent.VK_M);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuItem = new JMenuItem("Toggle colors", KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuDisplay.addSeparator();

        menuItem = new JMenuItem("Increase font dashboard", KeyEvent.VK_D);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuItem = new JMenuItem("Decrease font dashboard", KeyEvent.VK_F);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuItem = new JMenuItem("Increase font main display", KeyEvent.VK_I);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuItem = new JMenuItem("Decrease font main display", KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuDisplay.addSeparator();

        menuItem = new JMenuItem("Clear screen", KeyEvent.VK_S);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        menuItem = new JMenuItem("Write log", KeyEvent.VK_L);
        menuItem.addActionListener(this);
        menuDisplay.add(menuItem);

        this.setJMenuBar(menuBar);

        //Add display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);

        //displayArea.setTabSize(8);
        displayArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(displayArea);

        ContextMenuMouseListener mouseListener = new ContextMenuMouseListener(); // Add mouse context menu
        displayArea.addMouseListener(mouseListener);

        //Add display area
        displayAreaCurrent = new JTextArea();
        displayAreaCurrent.setEditable(false);
        displayAreaCurrent.setLineWrap(false);
        displayAreaCurrent.setTabSize(6);
        displayAreaCurrent.addMouseListener(mouseListener);
        JScrollPane scrollPaneCurrent = new JScrollPane(displayAreaCurrent);
        scrollPaneCurrent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        updateFonts();
        //Place button and text area on the screen
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(scrollPaneCurrent, BorderLayout.PAGE_END);

        //Startup!
        writeMenu();
        readMidiConfig();
        openMidiDevice();
        readSetlist();
        if (setlist.size() > 0) {
            runSetlistItemNumber(0);
        }
        displayArea.requestFocusInWindow();
        addBindings();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem) (e.getSource());
        String menuItem = source.getText();

        //Translate different menu options into differen actions:
        switch (menuItem) {
            case "Select midi config file":
                if (chooseConfigFileDialog()) {
                    readMidiConfig();
                    openMidiDevice();
                }
                break;
            case "Edit midi config":
                editMidiConfig();
                break;
            case "Reload midi config":
                readMidiConfig();
                openMidiDevice();
                break;
            case "Select midi device":
                selectMidiDeviceDialog();
                break;
            case "List midi devices":
                listMidiDevices();
                break;
            case "Open midi device":
                openMidiDevice();
                break;
            case "Close midi device":
                closeMidiDevice();
                break;
            case "List midi shortcuts available":
                displayMidiCodesAvailable();
                break;
            case "List midi shortcuts with codes":
                displayMidiCodes();
                break;
            case "Send midi test note":
                testSendMessage();
                break;
            case "Toggle midi check while sending":
                checkMidiDeviceAvailable = !checkMidiDeviceAvailable;
                break;
            case "Send midi code":
                sendMidiCodeDialog();
                break;
            case "Select setlist file":
                if (chooseSetlistFileDialog()) {
                    readSetlist();
                }
                break;
            case "Edit setlist":
                editSetlist();
                break;
            case "Reload setlist":
                readSetlist();
                break;
            case "Display setlist":
                displaySetlist();
                break;
            case "Check setlist":
                checkSetlist();
                break;
            case "Go to item...":
                chooseSetListItemDialog();
                break;
            case "Toggle colors":
                toggleColors();
                break;
            case "Toggle incoming midi":
                togglePrintMidiReceived();
                break;
            case "Clear screen":
                clearTextArea();
                break;
            case "Write log":
                writeLog();
                break;
            case "Increase font dashboard":
                increaseLargeFont();
                break;
            case "Decrease font dashboard":
                decreaseLargeFont();
                break;
            case "Increase font main display":
                increaseSmallFont();
                break;
            case "Decrease font main display":
                decreaseSmallFont();
                break;
        }
        displayArea.setCaretPosition(displayArea.getDocument().getLength());
    }

    // Returns just the class name -- no package info.
    protected String getClassName(Object o) {
        String classString = o.getClass().getName();
        int dotIndex = classString.lastIndexOf(".");
        return classString.substring(dotIndex + 1);
    }

    protected void addBindings() {

        Action up = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetlistPrevious();
            }
        };

        Action down = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetlistNext();
            }
        };

        Action downSilent = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                samplePlayer.isPlayingWav = false;
                isWaiting = false;
                runSetlistNext();
                samplePlayer.isPlayingWav = true;
                isWaiting = true;
            }
        };

        Action pageup = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetListPreviousPreset();
            }
        };

        Action pagedown = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetListNextPreset();
            }
        };

        Action home = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetlistStart();
            }
        };

        Action end = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetlistEnd();
            }
        };

        Action enter = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSetlistItemNumber(currentSetListIndex);
            }
        };
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "up");
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "up");

        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "down");
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "downSilent");
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "downSilent");

        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true), "pageup");
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true), "pagedown");

        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, true), "home");
        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0, true), "end");

        displayArea.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enter");

        displayArea.getActionMap().put("up", up);
        displayArea.getActionMap().put("down", down);
        displayArea.getActionMap().put("downSilent", downSilent);
        displayArea.getActionMap().put("pageup", pageup);
        displayArea.getActionMap().put("pagedown", pagedown);
        displayArea.getActionMap().put("home", home);
        displayArea.getActionMap().put("end", end);
        displayArea.getActionMap().put("enter", enter);

        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "up");
        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "up");

        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "down");
        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "downSilent");
        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "downSilent");

        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0, true), "pageup");
        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0, true), "pagedown");

        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, true), "home");
        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0, true), "end");

        displayAreaCurrent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), "enter");

        displayAreaCurrent.getActionMap().put("up", up);
        displayAreaCurrent.getActionMap().put("down", down);
        displayAreaCurrent.getActionMap().put("downSilent", downSilent);
        displayAreaCurrent.getActionMap().put("pageup", pageup);
        displayAreaCurrent.getActionMap().put("pagedown", pagedown);
        displayAreaCurrent.getActionMap().put("home", home);
        displayAreaCurrent.getActionMap().put("end", end);
        displayAreaCurrent.getActionMap().put("enter", enter);
    }

    ///////NAVIGATION////////////////
    // Goes to the beginning of the setlist
    public void runSetlistStart() {
        if (currentSetListIndex != 0) {
            runSetlistItemNumber(0);
        }
    }

    // Goes to the end of the setlist
    public void runSetlistEnd() {
        if (currentSetListIndex != setlist.size() - 1) {
            runSetlistItemNumber(setlist.size() - 1);
        }
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
            if (!setlist.get(previousPreset).getPreset().equals("")) {
                //found next preset
                previousPresetFound = true;
            }
        }
        if (previousPresetFound == true) {
            runSetlistItemNumber(previousPreset);
        }
    }

    // Goes to the next preset
    public void runSetListNextPreset() {
        boolean nextPresetFound = false;
        int nextPreset = currentSetListIndex;
        while (!nextPresetFound & nextPreset < setlist.size() - 1) {
            nextPreset++;

            if (!setlist.get(nextPreset).getPreset().equals("")) {
                //found next preset
                nextPresetFound = true;
                runSetlistItemNumber(nextPreset);
            }
        }
    }

    // Determine the strategy to go from the current set list index to a particular position and execute that strategy.
    public void runSetlistItemNumber(int goingTo) {

        int comingFrom = currentSetListIndex;

        if (goingTo == comingFrom) {

            //Case 1. We are not moving, just reapplying the current setting.
            runCurrentSetlistItem(false);
        } else if (goingTo > comingFrom) {

            //Case 2. We're moving downwards.
            //Is there a later preset we can start from?
            int latestPresetFound = comingFrom + 1;
            for (int i = latestPresetFound; i <= goingTo; i++) {
                if (!setlist.get(i).getPreset().equals("")) {
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
                if (!setlist.get(i).getPreset().equals("")) {
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
                    if (!setlist.get(previousPreset).getPreset().equals("")) {
                        presetFound = true;
                    } else {
                        previousPreset--;
                    }
                }

                //We need to go from presetFound up to goingTo.
                //We disable any samples being played here
                samplePlayer.isPlayingWav = false;
                isWaiting = false;
                for (int i = previousPreset; i <= goingTo; i++) {
                    currentSetListIndex = i;
                    runCurrentSetlistItem(false);
                }
                samplePlayer.isPlayingWav = true;
                isWaiting = true;
            }
        }
        // Add outcome to the display
        writeLine(currentSetListIndex + ": " + setlist.get(currentSetListIndex).toString());
    }

    //Run current set list item
    public void runCurrentSetlistItem(boolean reverseDirection) {
        System.out.println("Loading preset " + currentSetListIndex + ", reverse codes: " + reverseDirection);
//        if(reverseDirection == false){
//            writeLine(currentSetListIndex + ": " + setlist.get(currentSetListIndex).toString());
//        }
        SetlistItem item = setlist.get(currentSetListIndex);
        String[] midiCodes = item.getMidiCodes();
        String preset = item.getPreset();

        // Normal direction
        if (!reverseDirection) {

            // Send a studio set message if it is needed
            if (!preset.equals("")) {
                sendMessage(preset);
            }

            // Send other midi codes
            if (midiCodes != null) {
                for (String s : midiCodes) {

                    //Check whether we have a 'from > to'. Now we need the 'to'
                    String codeTo = s;
                    if (s.contains(">")) {
                        codeTo = s.split(">")[1];
                    }
                    sendMessage(codeTo);
                }
            }
        } else {

            // Reverse direction, so we reverse the codes!
            // Do we have codes?
            if (midiCodes != null) {

                // For each of the codes
                for (String s : item.getMidiCodes()) {

                    String codeTo = s;
                    if (s.contains(">")) {
                        //from>to overrides the defined opposites
                        codeTo = s.split(">")[0];
                    } else {
                        // Do we have an opposite code, compute it.
                        codeTo = computeOppositeMidiCode(s);
                        System.out.println("Opposite code for " + s + " is " + codeTo);
                    }
                    //Send the code (original if the reverse could not be computed
                    sendMessage(codeTo);
                }
            }
        }
    }

//Writes the menu options to the text area
    public void writeMenu() {
        writeLine("Config file: " + configFilename
                + newline + "Setlist file: " + setlistFilename
                + newline
                + newline + "Navigation:\tUse arrow keys to navigate (up/left for the previous item, right/down/space for the next item), or use CTRL-G and type number for a particular item"
                + newline + "\tUse page up for previous preset, page down for next preset, pedal for next item (if trigger configured as such), home for the first item, end for the last"
                + newline);
    }

    private boolean chooseConfigFileDialog() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            configFilename = file.getPath();
            return true;
        }
        return false;
    }

    public void editMidiConfig() {
        Desktop dt = Desktop.getDesktop();
        try {
            dt.open(new File(configFilename));
        } catch (IOException e) {
            writeLine("Could not load editor for " + setlistFilename);
        }
    }

    //Reads configuration file for midi device and codes
    public void readMidiConfig() {
        String deviceName = "";
        triggerlist = new ArrayList<>();

        BufferedReader reader = readFile(configFilename);
        String line = "";
        try {
            while (reader.ready()) {
                line = reader.readLine();
                //if not commented out with ;
                if (!line.startsWith(";") && !line.isEmpty()) {
                    String[] lineSplit = line.split(",");
                    if (lineSplit[0].equals("Device")) {
                        deviceName = lineSplit[1];
                        writeLine("Midi device configured: " + deviceName);
                        if (device != null) {
                            writeLine("Midi device currently opened: " + device.getDeviceInfo().getName());
                        }
                    } else if (lineSplit[0].equals("Trigger")) {

                        String key = lineSplit[1];
                        String triggerMessageType = lineSplit[2];
                        String[] triggerMessageContent = lineSplit[3].split("-");
                        boolean error = false;

                        int triggerChannel = 0;
                        int triggerControl = 0;
                        int triggerValue = 0;
                        String sysexCode = "";
                        String midiCode = "";

                        switch (triggerMessageType) {
                            case "CC":
                            case "PC":
                                if (triggerMessageContent.length > 1) {
                                    triggerChannel = intValue(triggerMessageContent[0]);
                                    triggerControl = intValue(triggerMessageContent[1]);
                                } else if (triggerMessageContent.length > 2) {
                                    triggerValue = intValue(triggerMessageContent[2]);
                                } else {
                                    error = true;
                                }
                                break;
                            case "SE":
                                sysexCode = triggerMessageContent[0];
                                break;
                            case "MC":
                                midiCode = triggerMessageContent[0];
                                break;
                        }

                        if (!error) {
                            MidiTrigger trigger = new MidiTrigger(key, triggerMessageType, triggerChannel, triggerControl, triggerValue, sysexCode, midiCode);
                            triggerlist.add(trigger);
                            writeLine(trigger.toString());
                        } else {
                            writeLine("Malformed midi code: " + line);
                        }
                    } else {
                        if (lineSplit.length >= 3) {
                            midiCodesMap.put(lineSplit[0], lineSplit[2]);
                            if (!lineSplit[1].equals("")) {
                                midiCodesOpposits.put(lineSplit[0], lineSplit[1]);
                            }
                        } else {
                            writeLine("Malformed midi code: " + line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            writeLine("Midi config error on line: " + line);
        }
        //Find the midi device
        selectMidiDevice(deviceName);
    }

    private void selectMidiDeviceDialog() {

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        Object[] options = new String[infos.length];
        for (int i = 0; i < infos.length; i++) {
            options[i] = infos[i].getName();
        }
        String s = (String) JOptionPane.showInputDialog(
                frame,
                "Midi devices available",
                "Select midi device",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        //If a string was returned, say so.
        if ((s != null) && (s.length() > 0)) {
            selectMidiDevice(s);
            openMidiDevice();
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
            write(s + " (" + midiCodesOpposits.get(s) + "), ");
        }
        writeLine("");
    }

    //Chooses a setlist file
    private boolean chooseSetlistFileDialog() {
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            setlistFilename = file.getPath();
            return true;
        }
        return false;
    }

    private void chooseSetListItemDialog() {

        String name = JOptionPane.showInputDialog(new JFrame(), "Which setlist item do you want to go to?");

        if (isNumeric(name)) {
            int value = intValue(name);
            if (value >= 0 && value <= setlist.size() - 1) {
                runSetlistItemNumber(value);
            } else {
                JOptionPane.showMessageDialog(this.frame, "This is not a present setlist item: " + name);
            }
        } else {
            JOptionPane.showMessageDialog(this.frame, "Not a number: " + name);
        }
    }

    private void sendMidiCodeDialog() {

        String message = JOptionPane.showInputDialog(null, "Send midi code");
        if (message != null) {
            if (!message.equals("")) {
                sendMessage(message);
                writeLine("Sending " + message);
            }
        }
    }

    //Read setlist from the selected file
    public void readSetlist() {
        BufferedReader reader = readFile(setlistFilename);
        String name = null;
        try {
            name = reader.readLine();
        } catch (Exception e) {
            writeLine("Reading error in midi config. Formatting: line one: device name, line two: pedal config (channel,cc,value), other lines: shortcut,midicode,opposite shortcut. Problem with line: " + name);
        }
        writeLine(newline + "Setlist: " + name);

        setlist = new ArrayList<>();
        String mostRecentSongName = "";
        HashMap<String, String> mostRecentMidiCodeLabels = null;
        try {
            while (reader.ready()) {
                name = reader.readLine();
                if (!name.startsWith(";") && !name.isEmpty()) {
                    String[] code = name.split(",");
                    SetlistItem item = new SetlistItem(code, mostRecentSongName, mostRecentMidiCodeLabels);
                    mostRecentSongName = item.getSongName();
                    mostRecentMidiCodeLabels = item.getMidiCodeLabels();
                    setlist.add(item);
                }
            }
        } catch (Exception e) {
            writeLine("Reading error in setlist. Formatting should be Songname,Measure,Preset,SoundNames or Songname,Measure,,Midicodes. Problem with line: " + name);
        }
        checkSetlist();
    }

    public void editSetlist() {
        Desktop dt = Desktop.getDesktop();
        try {
            dt.open(new File(setlistFilename));
        } catch (IOException e) {
            writeLine("Could not load editor for " + setlistFilename);
        }
    }

    //Writes out the setlist
    public void displaySetlist() {
        writeLine("Set list items:");
        for (SetlistItem item : setlist) {
            writeLine(item.toString());
        }
    }

    //Writes out the setlist
    //Writes out the setlist
    public void checkSetlist() {
        ArrayList<String> codes = new ArrayList<>();
//        writeLine("Checking for inconsistencies in setlist...");
        for (SetlistItem item : setlist) {
            if (!item.getPreset().equals("")) {
                codes = new ArrayList<>();
            } else {
                for (String code : item.getMidiCodes()) {

                    //if it has an opposite
                    String oppositeCode = midiCodesOpposits.get(code);
                    if (oppositeCode != null) {

                        if (!codes.contains(code) && !codes.contains(oppositeCode)) {
                            codes.add(code);
                        } else if (!codes.contains(code) && codes.contains(oppositeCode)) {
                            int i = codes.indexOf(oppositeCode);
                            codes.remove(i);
                            codes.add(code);
                        } else if (codes.contains(code) && !codes.contains(oppositeCode)) {
                            writeLine("Inconsistency, midi code " + code + " twice (without reversing) it in setlist item: " + item.toString());
                        }
                    }
                }
            }
        }
//        writeLine("Inconsistency check completed.");
    }

    //Finds the midi device     in the system with a name 
    public void selectMidiDevice(String name) {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        boolean found = false;

        for (int i = 0; i < infos.length; i++) {
            try {
                if (infos[i].getName().equals(name)) {
                    MidiDevice thisDevice = MidiSystem.getMidiDevice(infos[i]);
                    int r = thisDevice.getMaxReceivers();
                    int t = thisDevice.getMaxTransmitters();
                    if (t == 0) {
                        device = thisDevice;
                        found = true;
                    }
                    if (r == 0) {
                        deviceToListen = thisDevice;
                    }
                }
            } catch (MidiUnavailableException e) {
                writeLine("Could not open midi device.");
                errorColor();
            }
        }
        if (found == false) {
            writeLine("Didn't find the midi device");
            errorColor();
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
        if (device != null) {
            try {
                device.close();
                device.open();
                writeLine("Midi sender is now opened");
                listenerShouldBeReopened = true;
                errorResolvedColor();
            } catch (MidiUnavailableException e) {
                if (checkMidiDeviceAvailable) {
                    writeLine("Midi device couldn't be found");
                    errorColor();
                }
            }
        }

    }

    //Closes the currently selected midi device.
    public void closeMidiDevice() {
        writeLine("Closing currently selected device");
        if (device != null) {
            if (device.isOpen()) {
                try {
                    device.close();
                    writeLine("Device is now closed");
                } catch (Exception e) {
                    writeLine("Error closing midi device");
                }
            }
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
        }
        long timeStamp = -1;
        try {
            Receiver rcvr = device.getReceiver();
            writeLine("Sending test tone");
            rcvr.send(myMsg, timeStamp);
        } catch (Exception e) {
            writeLine("Error in sending test message");
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

    //Cleares the display area
    private void writeLog() {
        writeToFile("log.txt", displayArea.getText());
    }

    private void writeToFile(String filename, String text) {
        BufferedWriter writer = null;
        try {
            //System.getProperty("user.dir") + "/" + 
            writer = new BufferedWriter(new FileWriter(filename));
            writer.write(text);
            writeLine("Written to " + filename);

        } catch (IOException e) {
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void toggleColors() {

        if (displayArea.getBackground().equals(Color.WHITE)) {
            displayArea.setBackground(Color.BLACK);
            displayAreaCurrent.setBackground(Color.BLACK);
            displayArea.setForeground(Color.WHITE);
            displayAreaCurrent.setForeground(Color.WHITE);
        } else {
            displayArea.setBackground(Color.WHITE);
            displayAreaCurrent.setBackground(Color.WHITE);
            displayArea.setForeground(Color.BLACK);
            displayAreaCurrent.setForeground(Color.BLACK);
        }
    }

    public void errorColor() {
        displayAreaCurrent.setBackground(Color.RED);
    }

    public void errorResolvedColor() {
        displayAreaCurrent.setBackground(displayArea.getBackground());
    }

    public void increaseLargeFont() {
        largeFontSize++;
        updateFonts();
    }

    public void increaseSmallFont() {
        smallFontSize++;
        updateFonts();
    }

    public void decreaseLargeFont() {
        largeFontSize--;
        updateFonts();
    }

    public void decreaseSmallFont() {
        smallFontSize--;
        updateFonts();
    }

    public void updateFonts() {
        displayArea.setFont(new java.awt.Font("Arial", 0, smallFontSize));
        displayAreaCurrent.setFont(new java.awt.Font("Arial", 0, largeFontSize));
    }

    public void sendMessage(String message) {
        
        System.out.println("Sending: " + message);
        String messageBody = computeMidiCode(message);

        // Check whether the midi device is there
        if (checkMidiDeviceAvailable) {
            try {
                MidiSystem.getMidiDevice(device.getDeviceInfo());
            } catch (Exception e) {
                writeLine("Midi device seems unavailable, will try to reload. Use CTRL-W to try again or CTRL-U to suppress.");
                readMidiConfig();
                openMidiDevice();
            }
        }

        if (messageBody.startsWith("WAV")) {
            displayAreaCurrent.setText("WAV...");
            String[] messagePieces = messageBody.split("-");
            String fileName = messagePieces[1];
            samplePlayer.playSound(fileName);
        } else if (messageBody.startsWith("WAIT")) {
            String[] messagePieces = messageBody.split("-");
            int duration = intValue(messagePieces[1]);
            System.out.println(Thread.currentThread().getPriority() + " " + Thread.currentThread().getName());
            displayAreaCurrent.setText("WAIT...");
            if (isWaiting) {
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        } else if (messageBody.startsWith("PC") || messageBody.startsWith("CC")) {
            String[] messagePieces = messageBody.split("-");
            int channel = intValue(messagePieces[1]) - 1;
            int control = intValue(messagePieces[2]);
            int value = 0;
            int endValue = 0;
            int duration = 0;

            int messageType = 0;
            if (messageBody.startsWith("PC")) {
                messageType = ShortMessage.PROGRAM_CHANGE;
                //there is a from,to. So we select to.
                if (messagePieces.length >= 4) {
                    control = intValue(messagePieces[3]);
                }
            }
            if (messageBody.startsWith("CC")) {
                messageType = ShortMessage.CONTROL_CHANGE;

                value = intValue(messagePieces[3]);
                if (messagePieces.length == 5) {
                    value = intValue(messagePieces[4]);
                }
                if (messagePieces.length >= 6) {
                    value = intValue(messagePieces[3]);
                    endValue = intValue(messagePieces[4]);
                    duration = intValue(messagePieces[5]);
                }
            }

            if (duration > 0) {
                long startTime = System.currentTimeMillis();
                double elapsedTime = 0d;
                int oldValue = -1000;
                while (elapsedTime < duration) {

                    elapsedTime = (new Date()).getTime() - startTime;
                    double fraction = elapsedTime / (double) duration;
                    if (fraction > 1) {
                        fraction = 1;
                    } //cap the fraction.
                    int currentValue = (int) (value - (value - endValue) * fraction);

                    if (currentValue != oldValue) {
                        displayAreaCurrent.setText("Sending CC" + channel + " control " + control + " value " + currentValue);
                        sendShortMessage(messageType, channel, control, currentValue, messageBody);
                        oldValue = currentValue;
                    }
                }
            } else {
                sendShortMessage(messageType, channel, control, value, messageBody);
            }
        } else {
            SysexMessage sysmsg = new SysexMessage();
            try {
                String body = messageBody;
                int digits = 0;
                if (messageBody.contains(computedPlaceHolder)) {
                    digits = intValue(messageBody.split(" ")[0]);
                    String toRemove = digits + " ";
                    body = messageBody.replaceFirst(toRemove, "");
                }
                byte[] messageData = byteStringToByteArrayComputed(body, digits);
                sysmsg.setMessage(messageData, messageData.length);
            } catch (Exception ex) {
                writeLine("Error 6c: bad midi message: " + messageBody);
            }
            long timeStamp = -1;
            try {
                Receiver rcvr = device.getReceiver();
                rcvr.send(sysmsg, timeStamp);
            } catch (Exception e) {
                if (checkMidiDeviceAvailable) {
                    writeLine("Midi device not selected or opened");
                }
            }
        }
    }

    public void sendShortMessage(int messageType, int channel, int control, int value, String messageBody) {
        ShortMessage myMsg = new ShortMessage();

        try {
            myMsg.setMessage(messageType, channel, control, value);
        } catch (InvalidMidiDataException ex) {
            writeLine("Error making midi message " + myMsg.toString());
        }
        long timeStamp = -1;
        try {
            Receiver rcvr = device.getReceiver();
            rcvr.send(myMsg, timeStamp);
        } catch (Exception e) {
            if (checkMidiDeviceAvailable) {
                writeLine("Error in sending PC or CC message: type " + messageType + " channel " + channel + " control " + control + " value " + value);
            }
        }

    }

    public byte[] byteStringToByteArrayComputed(String s, int digits) {
        String[] stringArray = s.split(" ");
        int len = stringArray.length;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            String t = stringArray[i];
            int j = 0;
            if (t.contains("+" + computedPlaceHolder)) {
                j = (hexToDecimal(t.substring(0, 2)) + digits) % 128;
            } else if (t.contains("-" + computedPlaceHolder)) {
                j = (hexToDecimal(t.substring(0, 2)) + 128 - digits) % 128;
            } else {
                j = hexToDecimal(t);
            }
            data[i] = (byte) j;
            //System.out.print(decimalToHex(j) + " ");
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

    public void executeMidiTrigger(MidiTrigger trigger) {
        try {
            Robot robot = new Robot();

            // Simulate a key press
            robot.keyPress(trigger.keyEvent);
            robot.keyRelease(trigger.keyEvent);

        } catch (AWTException e) {
            writeLine("Problem with midi trigger");
        }
    }

    public static boolean isNumeric(String str) {
        if (str != null) {
            try {

                double d = Double.parseDouble(str);
            } catch (NumberFormatException nfe) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public static int intValue(String str) {
        int d;
        try {
            d = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
        return d;
    }

    public static int hexToDecimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        return val;
    }

    public static String decimalToHex(int d) {
        String digits = "0123456789ABCDEF";
        if (d == 0) {
            return "0";
        }
        String hex = "";
        while (d > 0) {
            int digit = d % 16;                // rightmost digit
            hex = digits.charAt(digit) + hex;  // string concatenation
            d = d / 16;
        }
        return hex;
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Configure the midi receiver
    public void listenToDevice() {

        // Wait until the display is setup
        while (displayArea == null) {
            try {
                //TODO work with ThreadPoolExecutor from: http://stackoverflow.com/questions/3535754/netbeans-java-new-hint-thread-sleep-called-in-loop
                // OR check https://ejrh.wordpress.com/2012/07/13/sleeping-in-loops-considered-harmful/
                Thread.sleep(10);
            } catch (InterruptedException e) {
                writeLine("Waiting");
            }
        }

        // Wait until deviceToListen is set
        if (deviceToListen != null) {

            //Open the listenerdevice. 
            openDeviceToListen();
            listenerShouldBeReopened = false;

            //Listener is now enabled. Wait until it needs reopening (after reloading midi config)
            while (!listenerShouldBeReopened) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    writeLine("Listener error 5");
                }
            }

            //Device needs to be reopened.
            deviceToListen.close();
        } else {
            // Wait
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                writeLine("Listener error 5");
            }
        }
    }

    public void openDeviceToListen() {
        // Skip if the deviceToListen is not set yet.
        if (deviceToListen != null) {
            try {
                deviceToListen.open();
                writeLine("Midi listener is opened");
            } catch (MidiUnavailableException ex) {
                writeLine("Midi device to listen to couldn't be opened");
            }
            receiver = new DumpReceiver(System.out, this);
            try {
                Transmitter t = deviceToListen.getTransmitter();
                t.setReceiver(receiver);
            } catch (MidiUnavailableException e) {
                writeLine("Error receiver 1");

            }
        }
    }

    private void handleKey(KeyEvent e) {
        //nothing!
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
                    midicontroller.displayArea.append("Error listening!");
                }
            }
        }
    }

    public String computeMidiCode(String code) {
        if (midiCodesMap.containsKey(code)) {
            return midiCodesMap.get(code);
        }

        String text = code.substring(0, lastNonNumeric(code) + 1);
        String digits = code.substring(lastNonNumeric(code) + 1, code.length());

        for (String s : midiCodesMap.keySet()) {
            if (s.contains(computedPlaceHolder)) {
                //get all before the x
                String s1 = s.split(computedPlaceHolder)[0];
                if (s1.equals(text)) {
                    return digits + " " + midiCodesMap.get(s);
                }
            }
        }
        return code;
    }

    public String computeOppositeMidiCode(String code) {
        if (midiCodesOpposits.containsKey(code)) {
            //System.out.println("Midi code configured as is: " + code);
            return midiCodesOpposits.get(code);
        }

        String text = code.substring(0, lastNonNumeric(code) + 1);
        String digits = code.substring(lastNonNumeric(code) + 1, code.length());

        for (String s : midiCodesOpposits.keySet()) {
            if (s.contains(computedPlaceHolder)) {

                //get all before the x
                String s1 = s.split(computedPlaceHolder)[0];
                if (s1.equals(text)) {
                    //System.out.println("Opposite code for text " + text + " for code " + code + " is code " + s + " with digits " + digits);
                    return midiCodesOpposits.get(s).replace(computedPlaceHolder, digits);
                }
            }
        }

        if (code.startsWith("CC")) {
            String[] messagePieces = code.split("-");
            // if the length is minimal 5, then we have a endvalue specified, means we can go back
            if (messagePieces.length >= 5) {
                return "CC-" + messagePieces[1] + "-" + messagePieces[2] + "-" + messagePieces[3];
            }
        }

        if (code.startsWith("PC")) {
            String[] messagePieces = code.split("-");
            // if the length is minimal 5, then we have a endvalue specified, means we can go back
            if (messagePieces.length >= 4) {
                return "PC-" + messagePieces[1] + "-" + messagePieces[2];
            }
        }

        if (code.startsWith("WAV")) {
            //Play no samples if going up.
            return "WAIT-0";
        }

        if (code.startsWith("WAIT")) {
            //Now waiting if going up.
            return "WAIT-0";
        }

        System.out.println("No opposite code for " + code);
        return code;
    }

    public static final int lastNonNumeric(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            Character c = s.charAt(i);
            if (!Character.isDigit(c)) {
                return i;
            }
        }
        return -1;
    }

}
