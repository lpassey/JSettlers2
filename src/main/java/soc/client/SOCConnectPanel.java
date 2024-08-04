/* *
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas <thomas@infolab.northwestern.edu>
 * This file copyright (C) 2008-2009,2012-2013,2017,2019-2022,2024 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2013 Paul Bilnoski <paul@bilnoski.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The maintainer of this program can be reached at jsettlers@nand.net
 **/
package soc.client;

import soc.util.SOCStringManager;
import soc.util.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This is the dialog panel for standalone client startup (JAR or otherwise)
 * if no command-line arguments.  Give choice of connect to server, start local server,
 * or create practice game.  Prompt for parameters for connect or start-server.
 *
 * @author Jeremy D Monin &lt;jeremy@nand.net&gt;
 * @since 1.1.00
 */
@SuppressWarnings("serial")
/*package*/ class SOCConnectPanel extends JPanel
    implements ActionListener, KeyListener
{
    private final MainDisplay md;
    private final ClientNetwork clientNetwork;

    /** Welcome message, or error after disconnect */
    private JLabel topText;

    /** "Practice" */
//    private JButton prac;

     /** "Connect to server" */
//    private JButton connserv;

    /** Contains GUI elements for details in {@link #conn_connect} */
    private JPanel panel_conn;
    private JTextField conn_servhost, conn_servport, conn_user;
    private JPasswordField conn_pass;
    private JButton conn_connect;
    //, conn_cancel;

    private static final Color HEADER_LABEL_BG = new Color(220,255,220);
    private static final Color HEADER_LABEL_FG = new Color( 50, 80, 50);

    /**
     * i18n text strings; will use same locale as SOCPlayerClient's string manager.
     * @since 2.0.00
     */
    private static final SOCStringManager strings = SOCStringManager.getClientManager();

    /**
     * Creates a new SOCConnectOrPracticePanel.
     *
     * @param md      Player client main display
     */
    public SOCConnectPanel( final MainDisplay md)
    {
        super(new BorderLayout());

        this.md = md;
        SOCPlayerClient cli = md.getClient();
        clientNetwork = cli.getNet();

        // same Frame setup as in SOCPlayerClient.main
        final Color[] colors = SwingMainDisplay.getForegroundBackgroundColors(false, false);
        if (colors != null)
        {
            setBackground(colors[2]);  // SwingMainDisplay.JSETTLERS_BG_GREEN
            setForeground(colors[0]);  // Color.BLACK
        }

        addKeyListener(this);
        initInterfaceElements(colors != null ? colors[1] : null);  // SwingMainDisplay.MISC_LABEL_FG_OFF_WHITE
    }

    /**
     * Interface setup for constructor.
     * Most elements are part of a sub-panel occupying most of this Panel, using a vertical BoxLayout.
     * There's also a label at bottom with the version and build number.
     * @param miscLabelFGColor  Foreground color for miscellaneous label text, or {@code null} for panel's text color;
     *     typically {@link SwingMainDisplay#MISC_LABEL_FG_OFF_WHITE}
     */
    private void initInterfaceElements(final Color miscLabelFGColor)
    {
        // The actual content of this dialog is bp, a narrow stack of buttons and other UI elements.
        // This stack is centered horizontally in the larger container, and doesn't fill the entire width.
        // Since the content pane's BorderLayout wants to stretch things to fill its center,
        // to leave space on the left and right we wrap bp in a larger bpContainer ordered horizontally.

        final JPanel bpContainer = new JPanel();
        bpContainer.setLayout(new BoxLayout(bpContainer, BoxLayout.X_AXIS));
        bpContainer.setBackground(null);  // inherit from parent
        bpContainer.setForeground(null);

        final boolean isOSHighContrast = SwingMainDisplay.isOSColorHighContrast();

        /**
         * JButton.setBackground(null) is needed on win32 to avoid gray corners
         */
        final boolean shouldClearButtonBGs = (! isOSHighContrast) && SOCPlayerClient.IS_PLATFORM_WINDOWS;

        // In center of bpContainer, bp holds the narrow UI stack:
        final JPanel bp = new BoxedJPanel();
        bp.setLayout(new BoxLayout(bp, BoxLayout.Y_AXIS));
        if (! isOSHighContrast)
        {
            bp.setBackground(null);
            bp.setForeground(null);
        }
        bp.setAlignmentX(CENTER_ALIGNMENT);  // center bp within entire content pane

        // The welcome label and 3 buttons should be the same width,
        // so they get a sub-panel of their own using GBL:

        final GridBagLayout gbl = new GridBagLayout();
        final GridBagConstraints gbc = new GridBagConstraints();

        /*
          This "modeButtonsContainer" hold the three buttons to connect to a server, launch
          a practice server, or launch an in memory server. This client can't do two of these
          things, so we just skip it and show the connect panel
         */
//        final JPanel modeButtonsContainer = new BoxedJPanel(gbl);
//        if (! isOSHighContrast)
//        {
//            modeButtonsContainer.setBackground(null);
//            modeButtonsContainer.setForeground(null);
//        }
//
//        gbc.fill = GridBagConstraints.BOTH;
//        gbc.gridwidth = GridBagConstraints.REMAINDER;
//
//        topText = new JLabel(strings.get("pcli.cpp.welcomeheading"), SwingConstants.CENTER);
//            // "Welcome to JSettlers!  Please choose an option."
//        gbl.setConstraints(topText, gbc);
//        modeButtonsContainer.add(topText);
//
//        bp.add(modeButtonsContainer);

        /**
         * Interface setup: sub-panels (not initially visible)
         */
        panel_conn = initInterface_conn();  // panel_conn setup
        panel_conn.setVisible( true );
        bp.add (panel_conn);

        // Final assembly setup
        bpContainer.add(Box.createHorizontalGlue());
        bpContainer.add(bp);
        bpContainer.add(Box.createHorizontalGlue());

        add(bpContainer, BorderLayout.CENTER);
        JLabel verl = new JLabel
            (strings.get("pcli.cpp.jsettlers.versionbuild", Version.version(), Version.buildnum()), SwingConstants.CENTER);
            // "JSettlers " + Version.version() + " build " + Version.buildnum()
        if (miscLabelFGColor != null)
            verl.setForeground(miscLabelFGColor);
        add(verl, BorderLayout.SOUTH);
    }

    /** panel_conn setup */
    private JPanel initInterface_conn()
    {
        final boolean isOSHighContrast = SwingMainDisplay.isOSColorHighContrast();
        final boolean shouldClearButtonBGs = (! isOSHighContrast) && SOCPlayerClient.IS_PLATFORM_WINDOWS;
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel pconn = new BoxedJPanel(gbl);

        if (! isOSHighContrast)
        {
            pconn.setBackground(null);  // inherit from parent
            pconn.setForeground(null);
        }

        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel L;

        // heading row

        L = new JLabel(strings.get("pcli.cpp.connecttoserv"), SwingConstants.CENTER);  // "Connect to Server"
        if (! isOSHighContrast)
        {
            L.setBackground(HEADER_LABEL_BG);
            L.setForeground(HEADER_LABEL_FG);
            L.setOpaque(true);
        }
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.ipady = 8;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        gbc.ipady = 0;

        // field rows

        L = new JLabel(strings.get("pcli.cpp.server"));
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_servhost = new JTextField(20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_servhost, gbc);
        conn_servhost.addKeyListener(this);   // for ESC/ENTER
        pconn.add(conn_servhost);

        L = new JLabel(strings.get("pcli.cpp.port"));
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_servport = new JTextField(20);
        {
            String svp = Integer.toString(clientNetwork.getPort());
            conn_servport.setText(svp);
            conn_servport.setSelectionStart(0);
            conn_servport.setSelectionEnd(svp.length());
        }
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_servport, gbc);
        conn_servport.addKeyListener(this);   // for ESC/ENTER
        pconn.add(conn_servport);

        L = new JLabel(strings.get("pcli.cpp.nickname"));
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_user = new JTextField(20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_user, gbc);
        conn_user.addKeyListener(this);
        pconn.add(conn_user);

        L = new JLabel(strings.get("pcli.cpp.password"));
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_pass = new JPasswordField(20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_pass, gbc);
        conn_pass.addKeyListener(this);
        pconn.add(conn_pass);

        // button row

        L = new JLabel("");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_connect = new JButton(strings.get("pcli.cpp.connect"));
        if (shouldClearButtonBGs)
            conn_connect.setBackground(null);
        conn_connect.addActionListener(this);
        conn_connect.addKeyListener(this);  // for win32 keyboard-focus
        gbc.weightx = 0.5;
        gbl.setConstraints(conn_connect, gbc);
        pconn.add(conn_connect);

        return pconn;
    }

   /**
    * TODO: Do nothing method to keep SOCMainDisplay from breaking ... for now
    *  Called from {@link MainDisplay#startLocalTCPServer(int)}, only implemented
    *  in {@link SwingServerMainDisplay}
    */
    public void startedLocalServer()
    {
    }

    /**
     * TODO: Currently unused. when it becomes used, switch to the Connect card and try to
     * connect again or to a different server
     * <p>
     * We were connected to a TCP server (remote, or the one we started) but something broke the connection.
     * Show an error message and the initial 3 buttons, as if we've just started the client up.
     *
     * @param errText  Error message to show. Can be multi-line by using sanitized html with {@code <BR>} tag
     *     and starting with {@code <HTML>} tag, as done when using a {@link JLabel}.
     * @since 2.5.00
     */
    public void lostServerConnection(final String errText)
    {
        // Hide any visible detail fields
        setTopText(errText);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        for (JButton b : new JButton[]{ conn_connect
        })
        {
            b.setEnabled( true );
        }
    }

    /**
     * Set the line of text displayed at the top of the panel.
     * @param newText  New text to display. Can be multi-line by using sanitized html with {@code <BR>} tag
     *     and starting with {@code <HTML>} tag, as done when using a {@link JLabel}.
     * @since 1.1.16
     */
    public void setTopText(final String newText)
    {
        topText.setText(newText);
        validate();
    }

    /**
     * Update the contents of the panel's Host and Port fields.
     * @param chost Hostname to set to, or {@code null} to leave unchanged
     * @param cport Port number to set to
     * @since 2.7.00
     */
    public void setServerHostPort(final String chost, final int cport)
    {
        if ((chost != null) && ! conn_servhost.getText().equals(chost))
            conn_servhost.setText(chost);

        String pstr = String.valueOf(cport);
        if (! conn_servport.getText().equals(pstr))
            conn_servport.setText(pstr);
    }

    /**
     * Parse a server TCP port number from a text field.
     * If the field is empty after trimming whitespace, use this client's default from
     * {@link ClientNetwork#getPort() clientNetwork.getPort()},
     * which is usually {@link ClientNetwork#SOC_PORT_DEFAULT}.
     * @param portNumber   Text field with the port number
     * @return the port number, or {@code clientNetwork.getPort()} if empty,
     *         or 0 if cannot be parsed or if outside the valid range 1-65535
     * @since 1.1.19
     */
    private int parsePortNumberOrDefault( final JTextField portNumber )
    {
        int srport;
        try {
            final String ptext = portNumber .getText().trim();
            if (ptext.length() > 0)
                srport = Integer.parseInt(ptext);
            else
                srport = clientNetwork.getPort();  // text field is empty, use default (usually == SOC_PORT_DEFAULT)

            if ((srport <= 0) || (srport > 65535))
                srport = 0;  // TODO show error
        }
        catch (NumberFormatException e)
        {
            // TODO show error?
            srport = 0;
        }

        return srport;
    }

    /** React to button clicks */
    public void actionPerformed(ActionEvent ae)
    {
        try {

        Object src = ae.getSource();
//        if (src == prac)
//        {
//            // Ask client to set up and start a practice game
//            md.clickPracticeButton();
//            return;
//        }

//        if (src == connserv)
//        {
//            // Show fields to get details to connect to server later
//            panel_conn.setVisible(true);
//            connserv.setVisible(false);
//            conn_servhost.requestFocus();
//            validate();
//            return;
//        }

            if (src == conn_connect)
            {
                // After clicking connserv, actually connect to server
                clickConnConnect();
            }


        }  // try
        catch( Throwable thr )
        {
            System.err.println("-- Error caught in AWT event thread: " + thr + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }
    }

    /** "Connect..." from connect setup; check fields, set WAIT_CURSOR, ask cli to connect  */
    private void clickConnConnect()
    {
        // TODO Check contents of fields
        String cserv = conn_servhost.getText().trim();
        if (cserv.length() == 0)
            cserv = null;  // localhost
        final int cport = parsePortNumberOrDefault(conn_servport);
        if (cport == 0)
        {
            return;  // <--- Early return: Couldn't parse port number ---
        }

        // Copy fields, show MAIN_PANEL, and connect in client
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        md.getClient().connect(cserv, cport, conn_user.getText(), String.valueOf( conn_pass.getPassword() ));
    }

    /** Hide fields used to connect to server. */
//    private void clickConnCancel()

    /** Actually start a server, on port from {@link #run_servport} */
//    private void clickRunStartserv()

    /** Hide fields used to connect to server. */
//    private void clickConnCancel()
//    {
//        validate();
//    }

    /** Hide fields used to start a server */
//    private void clickRunCancel()

    /** Handle Enter or Esc key (KeyListener) */
    public void keyPressed(KeyEvent e)
    {
        if (e.isConsumed())
            return;

        try {

        boolean panelConnShowing = (panel_conn != null) && (panel_conn.isVisible());

        switch (e.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
            if (panelConnShowing)
                clickConnConnect();
            break;

        case KeyEvent.VK_CANCEL:
        case KeyEvent.VK_ESCAPE:
//            if (panelConnShowing)
//                clickConnCancel();
            break;
        }  // switch(e)

        }  // try
        catch(Throwable thr)
        {
            System.err.println("-- Error caught in AWT event thread: " + thr + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }
    }

    /** Stub required by KeyListener */
    public void keyReleased(KeyEvent arg0) { }

    /** Stub required by KeyListener */
    public void keyTyped(KeyEvent arg0) { }

    /**
     * {@link JPanel} for use in {@link BoxLayout}; overrides {@link #getMaximumSize()}
     * to prevent some unwanted extra width. JPanel's default max is 32767 x 32767
     * and our container's BoxLayout adds some proportion of that,
     * based on its overall container width beyond our minimum/preferred width.
     *
     * @author jdmonin
     * @since 2.0.00
     */
    static final class BoxedJPanel extends JPanel
    {
        public BoxedJPanel() { super(); }
        public BoxedJPanel(LayoutManager m) { super(m); }

        @Override
        public Dimension getMaximumSize() { return getPreferredSize(); }
    }
}
