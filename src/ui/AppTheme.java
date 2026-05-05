package ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * Consistent colors, type, and widgets so the UI reads as one product rather than default Swing.
 */
public final class AppTheme {

    /** Warm paper workspace */
    public static final Color PAGE = new Color(0xEA, 0xE5, 0xDA);
    /** Card surface */
    public static final Color CARD = new Color(0xFF, 0xFA, 0xF5);
    public static final Color INPUT_BG = Color.WHITE;
    public static final Color INK = new Color(0x22, 0x20, 0x1F);
    public static final Color MUTED = new Color(0x6E, 0x69, 0x63);
    /** Deep evergreen — primary accent */
    public static final Color ACCENT = new Color(0x1F, 0x53, 0x47);
    public static final Color ACCENT_HOVER = new Color(0x2A, 0x6F, 0x5F);
    /** Warm contrast stripe (login) */
    public static final Color RUST = new Color(0xB5, 0x5D, 0x3F);
    public static final Color STROKE = new Color(0xCB, 0xC5, 0xBA);
    public static final Color GRID = new Color(0xE4, 0xDF, 0xD7);
    public static final Color TABLE_HEADER_BG = new Color(0x1A, 0x44, 0x39);

    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font BODY_SEMI = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    private AppTheme() {}

    /** Must run before constructing any Swing component (typically first line inside invokeLater). */
    public static void install() throws Exception {
        boolean nimbusOk = false;
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                nimbusOk = true;
                break;
            }
        }
        if (!nimbusOk) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }

        javax.swing.plaf.FontUIResource bodyRes = new javax.swing.plaf.FontUIResource(BODY);
        javax.swing.plaf.FontUIResource semiRes = new javax.swing.plaf.FontUIResource(BODY_SEMI);
        javax.swing.UIManager.put("defaultFont", bodyRes);

        javax.swing.UIManager.put("Panel.background", PAGE);
        javax.swing.UIManager.put("Panel.font", bodyRes);

        javax.swing.UIManager.put("MenuBar.background", CARD);
        javax.swing.UIManager.put("Menu.background", CARD);
        javax.swing.UIManager.put("Menu.foreground", INK);
        javax.swing.UIManager.put("MenuItem.background", CARD);
        javax.swing.UIManager.put("MenuItem.foreground", INK);
        javax.swing.UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(STROKE));

        javax.swing.UIManager.put("Label.font", bodyRes);
        javax.swing.UIManager.put("Label.foreground", INK);
        javax.swing.UIManager.put("Button.font", semiRes);

        javax.swing.UIManager.put("TextField.background", INPUT_BG);
        javax.swing.UIManager.put("FormattedTextField.background", INPUT_BG);
        javax.swing.UIManager.put("PasswordField.background", INPUT_BG);

        javax.swing.UIManager.put("Table.background", CARD);
        javax.swing.UIManager.put("Table.foreground", INK);
        javax.swing.UIManager.put("Table.gridColor", GRID);
        javax.swing.UIManager.put("Table.font", bodyRes);

        javax.swing.UIManager.put("TableHeader.background", TABLE_HEADER_BG);
        javax.swing.UIManager.put("TableHeader.foreground", Color.WHITE);

        javax.swing.UIManager.put("CheckBox.foreground", INK);
        javax.swing.UIManager.put("CheckBox.background", PAGE);

        javax.swing.UIManager.put("ComboBox.background", INPUT_BG);

        javax.swing.UIManager.put("OptionPane.background", PAGE);
        javax.swing.UIManager.put("TextArea.background", INPUT_BG);

        javax.swing.UIManager.put("scrollPaneBorder", BorderFactory.createLineBorder(STROKE));

        javax.swing.UIManager.put("control", CARD);
        javax.swing.UIManager.put("text", INK);

        javax.swing.UIManager.put("nimbusOrange", ACCENT_HOVER);
        javax.swing.UIManager.put("nimbusGreen", ACCENT);
        javax.swing.UIManager.put("nimbusBlueGrey", new Color(0x72, 0x7F, 0x7D));
        javax.swing.UIManager.put("nimbusFocus", ACCENT_HOVER);

        Font menuFont = BODY.deriveFont(12f);
        javax.swing.UIManager.put("Menu.font", menuFont);
        javax.swing.UIManager.put("MenuItem.font", BODY);
        javax.swing.UIManager.put("TextArea.font",
                new Font(Font.MONOSPACED, Font.PLAIN, 12));

        Font tableHeaderActual = BODY_SEMI;
        javax.swing.UIManager.put("TableHeader.font", tableHeaderActual);
    }

    public static Font displayFont(float sizePx) {
        return new Font(Font.SERIF, Font.BOLD, Math.round(sizePx));
    }

    /** Page background + refreshed menu chrome. */
    public static void polishFrame(JFrame frame) {
        Container c = frame.getContentPane();
        if (c != null) {
            c.setBackground(PAGE);
        }
        frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (frame.getJMenuBar() != null) {
            styleMenuBar(frame.getJMenuBar());
        }
    }

    public static void styleMenuBar(JMenuBar bar) {
        bar.setOpaque(true);
        bar.setBackground(CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, STROKE),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(ACCENT);
        l.setFont(BODY_SEMI);
        return l;
    }

    public static JLabel caption(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(BODY);
        return l;
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        stylePrimary(b);
        return b;
    }

    public static void stylePrimary(AbstractButton b) {
        b.setOpaque(true);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        styleSecondary(b);
        return b;
    }

    public static void styleSecondary(AbstractButton b) {
        b.setOpaque(true);
        b.setBackground(CARD);
        b.setForeground(ACCENT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Compact controls used in dense tool strips. */
    public static void styleCompact(AbstractButton b) {
        b.setOpaque(true);
        b.setBackground(new Color(0xF0, 0xEB, 0xE3));
        b.setForeground(INK);
        b.setFont(BODY.deriveFont(11.5f));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleTextField(JTextField f) {
        f.setBackground(INPUT_BG);
        f.setForeground(INK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        f.setFont(BODY);
    }

    public static void stylePassword(JPasswordField f) {
        f.setBackground(INPUT_BG);
        f.setForeground(INK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        f.setFont(BODY);
    }

    public static void styleCombo(JComboBox<?> c) {
        c.setBackground(INPUT_BG);
        c.setForeground(INK);
        c.setFont(BODY);
    }

    public static void styleCheckBox(JCheckBox cb) {
        cb.setOpaque(false);
        cb.setForeground(INK);
        cb.setFont(BODY);
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(26);
        t.setShowGrid(true);
        t.setGridColor(GRID);
        t.setBackground(CARD);
        t.setSelectionBackground(new Color(0x3D, 0x8B, 0x72, 70));
        t.setSelectionForeground(INK);
        t.setFont(BODY);
        if (t.getTableHeader() != null) {
            t.getTableHeader().setBackground(TABLE_HEADER_BG);
            t.getTableHeader().setForeground(Color.WHITE);
            t.getTableHeader().setFont(BODY_SEMI);
            t.getTableHeader().setReorderingAllowed(false);
        }
    }

    public static JScrollPane wrapTable(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        sp.getViewport().setBackground(CARD);
        return sp;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE, 1),
                BorderFactory.createEmptyBorder(16, 20, 16, 20));
    }

    public static TitledBorder titled(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(STROKE), title, TitledBorder.LEFT, TitledBorder.TOP,
                BODY_SEMI, ACCENT);
        tb.setTitlePosition(TitledBorder.ABOVE_TOP);
        return tb;
    }

    /** Light card on page; call setBackground(CARD) on panel first. */
    public static void asCard(JPanel p, String title) {
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                titled(title),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
    }

    public static void styleTextArea(JTextArea a) {
        a.setBackground(INPUT_BG);
        a.setForeground(INK);
        a.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STROKE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        a.setCaretColor(ACCENT);
    }

    public static JScrollPane wrapTextArea(JTextArea a) {
        styleTextArea(a);
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createLineBorder(STROKE));
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return sp;
    }

}
