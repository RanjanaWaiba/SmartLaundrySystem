package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Visual representation of one laundry resource.
 *
 * States:
 * - IDLE
 * - BUSY / ACQUIRED BY CUSTOMER
 * - FAILED
 * - OUT OF ORDER
 */
public class MachineTile extends JPanel {

    public enum TileType {
        WASHER,
        DRYER,
        KIOSK
    }

    // ============================================================
    // COMMON COLOURS
    // ============================================================

    private static final Color IDLE_BG =
            new Color(248, 250, 252);

    private static final Color IDLE_BORDER =
            new Color(226, 232, 240);

    private static final Color IDLE_TEXT =
            new Color(71, 85, 105);

    private static final Color IDLE_DOT =
            new Color(148, 163, 184);

    // ============================================================
    // DARK ACQUIRED COLOURS
    // ============================================================

    // Washer -> dark green
    private static final Color WASHER_DARK =
            new Color(6, 95, 70);

    private static final Color WASHER_DARK_BORDER =
            new Color(4, 78, 57);

    // Dryer -> dark blue
    private static final Color DRYER_DARK =
            new Color(30, 64, 175);

    private static final Color DRYER_DARK_BORDER =
            new Color(23, 49, 136);

    // Kiosk -> dark orange
    private static final Color KIOSK_DARK =
            new Color(146, 64, 14);

    private static final Color KIOSK_DARK_BORDER =
            new Color(120, 53, 12);

    // ============================================================
    // FAILURE
    // ============================================================

    private static final Color FAILED_BG =
            new Color(254, 226, 226);

    private static final Color FAILED_BORDER =
            new Color(220, 38, 38);

    private static final Color FAILED_TEXT =
            new Color(185, 28, 28);

    // ============================================================
    // OUT OF ORDER
    // ============================================================

    private static final Color OUT_BG =
            new Color(241, 245, 249);

    private static final Color OUT_BORDER =
            new Color(100, 116, 139);

    private static final Color OUT_TEXT =
            new Color(71, 85, 105);

    // ============================================================
    // COMPONENTS
    // ============================================================

    private final JLabel nameLabel =
            new JLabel();

    private final JLabel statusLabel =
            new JLabel();

    private final JLabel dotLabel =
            new JLabel("●");

    private final TileType type;

    private Color busyBackground;
    private Color busyBorder;

    private Color currentBorder =
            IDLE_BORDER;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public MachineTile(
            String name,
            TileType type
    ) {

        this.type = type;

        configurePalette(type);

        build(name);

        setIdle();
    }

    // ============================================================
    // PALETTE
    // ============================================================

    /**
     * Selects the dark colour used when this machine
     * is acquired by a customer.
     *
     * Traditional switch syntax is used for Java compatibility.
     */
    private void configurePalette(
            TileType type
    ) {

        switch (type) {

            case WASHER:

                busyBackground =
                        WASHER_DARK;

                busyBorder =
                        WASHER_DARK_BORDER;

                break;

            case DRYER:

                busyBackground =
                        DRYER_DARK;

                busyBorder =
                        DRYER_DARK_BORDER;

                break;

            case KIOSK:

                busyBackground =
                        KIOSK_DARK;

                busyBorder =
                        KIOSK_DARK_BORDER;

                break;

            default:

                busyBackground =
                        IDLE_BG;

                busyBorder =
                        IDLE_BORDER;

                break;
        }
    }

    // ============================================================
    // BUILD TILE
    // ============================================================

    private void build(
            String name
    ) {

        setOpaque(false);

        setLayout(
                new BorderLayout(
                        8,
                        6
                )
        );

        setBorder(
                new EmptyBorder(
                        10,
                        11,
                        10,
                        11
                )
        );

        setPreferredSize(
                new Dimension(
                        145,
                        92
                )
        );

        setMinimumSize(
                new Dimension(
                        118,
                        82
                )
        );

        // --------------------------------------------------------
        // Top row
        // --------------------------------------------------------

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setOpaque(false);

        nameLabel.setText(
                name
        );

        nameLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        top.add(
                nameLabel,
                BorderLayout.WEST
        );

        // --------------------------------------------------------
        // Status dot
        // --------------------------------------------------------

        dotLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        11
                )
        );

        dotLabel.setHorizontalAlignment(
                SwingConstants.RIGHT
        );

        top.add(
                dotLabel,
                BorderLayout.EAST
        );

        // --------------------------------------------------------
        // Status
        // --------------------------------------------------------

        statusLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        statusLabel.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        add(
                top,
                BorderLayout.NORTH
        );

        add(
                statusLabel,
                BorderLayout.CENTER
        );
    }

    // ============================================================
    // ROUNDED CARD PAINTING
    // ============================================================

    @Override
    protected void paintComponent(
            Graphics g
    ) {

        Graphics2D g2 =
                (Graphics2D) g.create();

        try {

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Shape card =
                    new RoundRectangle2D.Double(
                            0.5,
                            0.5,
                            getWidth() - 1,
                            getHeight() - 1,
                            16,
                            16
                    );

            g2.setColor(
                    getBackground()
            );

            g2.fill(
                    card
            );

            g2.setColor(
                    currentBorder
            );

            g2.draw(
                    card
            );

        } finally {

            g2.dispose();
        }

        super.paintComponent(
                g
        );
    }

    // ============================================================
    // APPLY STATE
    // ============================================================

    private void applyState(
            Color background,
            Color border,
            Color nameColor,
            Color textColor,
            String status,
            Color dotColor
    ) {

        setBackground(
                background
        );

        currentBorder =
                border;

        nameLabel.setForeground(
                nameColor
        );

        statusLabel.setForeground(
                textColor
        );

        statusLabel.setText(
                status
        );

        dotLabel.setForeground(
                dotColor
        );

        repaint();
    }

    // ============================================================
    // IDLE
    // ============================================================

    public void setIdle() {

        applyState(
                IDLE_BG,
                IDLE_BORDER,
                IDLE_TEXT,
                IDLE_TEXT,
                "IDLE",
                IDLE_DOT
        );
    }

    // ============================================================
    // BUSY / ACQUIRED
    // ============================================================

    public void setBusy(
            int customerId
    ) {

        /*
         * Dark background clearly indicates that the machine
         * has been acquired by a customer.
         */
        applyState(
                busyBackground,
                busyBorder,
                Color.WHITE,
                Color.WHITE,
                "CUSTOMER " + customerId,
                Color.WHITE
        );
    }

    // ============================================================
    // FAILED
    // ============================================================

    public void setFailed(
            int customerId
    ) {

        applyState(
                FAILED_BG,
                FAILED_BORDER,
                FAILED_TEXT,
                FAILED_TEXT,
                "FAILED • C" + customerId,
                FAILED_TEXT
        );
    }

    // ============================================================
    // OUT OF ORDER
    // ============================================================

    public void setOutOfOrder() {

        applyState(
                OUT_BG,
                OUT_BORDER,
                OUT_TEXT,
                OUT_TEXT,
                "OUT OF ORDER",
                OUT_BORDER
        );
    }
}