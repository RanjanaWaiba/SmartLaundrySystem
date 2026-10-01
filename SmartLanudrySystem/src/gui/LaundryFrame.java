package gui;

import event.MachineType;
import event.SimulationListener;
import manager.ResourceManager;
import manager.StatisticsManager;
import util.ConfigLoader;
import util.TimeUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Modern Smart Laundry Facility dashboard.
 *
 * Design goals:
 * - no unnecessary academic footer
 * - modern teal header
 * - clear machine ownership
 * - concise statistics
 * - visible waiting queues
 * - visible failure information
 * - thread-labelled live activity trace
 * - Swing updates performed on the Event Dispatch Thread
 * - large centred congestion alert
 */
public class LaundryFrame
        extends JFrame
        implements SimulationListener {

    // ============================================================
    // COLOURS
    // ============================================================

    private static final Color APP_BG =
            new Color(248, 250, 252);

    private static final Color WHITE =
            Color.WHITE;

    private static final Color TEXT =
            new Color(15, 23, 42);

    private static final Color MUTED =
            new Color(100, 116, 139);

    private static final Color BORDER =
            new Color(226, 232, 240);

    /*
     * Modern teal header.
     */
    private static final Color TEAL =
            new Color(13, 148, 136);

    private static final Color TEAL_DARK =
            new Color(15, 118, 110);

    private static final Color GREEN =
            new Color(16, 185, 129);

    private static final Color BLUE =
            new Color(59, 130, 246);

    private static final Color ORANGE =
            new Color(245, 158, 11);

    private static final Color RED =
            new Color(220, 38, 38);

    private static final Color PURPLE =
            new Color(124, 58, 237);

    private static final Color SOFT_TEAL =
            new Color(204, 251, 241);

    private static final Color SOFT_GREEN =
            new Color(236, 253, 245);

    private static final Color SOFT_BLUE =
            new Color(239, 246, 255);

    private static final Color SOFT_ORANGE =
            new Color(255, 251, 235);

    private static final Color SOFT_PURPLE =
            new Color(245, 243, 255);

    // ============================================================
    // SHARED STATE
    // ============================================================

    private final StatisticsManager stats;

    private final ResourceManager resources;

    private final Map<String, MachineTile> tileMap =
            new HashMap<>();

    private final boolean congestedMode;

    private final long startTime;

    // ============================================================
    // HEADER LABELS
    // ============================================================

    private JLabel timerLabel;

    private JLabel modeLabel;

    private JLabel liveLabel;

    // ============================================================
    // STATISTICS LABELS
    // ============================================================

    private JLabel arrivedValue;

    private JLabel servedValue;

    private JLabel averageValue;

    private JLabel activeValue;

    // ============================================================
    // QUEUE LABELS
    // ============================================================

    private JLabel washerQueueValue;

    private JLabel dryerQueueValue;

    private JLabel paymentQueueValue;

    // ============================================================
    // SAFETY LABELS
    // ============================================================

    private JLabel washerPeakValue;

    private JLabel dryerPeakValue;

    private JLabel washerFailureValue;

    private JLabel paymentFailureValue;

    // ============================================================
    // ACTIVITY / ALERT
    // ============================================================

    private JLabel alertBanner;

    private JTextArea activityArea;

    // ============================================================
    // TIMERS
    // ============================================================

    private Timer statTimer;

    private Timer clockTimer;

    private boolean finished;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public LaundryFrame(
            StatisticsManager stats,
            ResourceManager resources,
            long startTime,
            boolean congestedMode
    ) {

        super(
                "Smart Laundry Facility"
        );

        this.stats =
                stats;

        this.resources =
                resources;

        this.startTime =
                startTime;

        this.congestedMode =
                congestedMode;

        configureWindow();

        buildUI();

        installTimers();

        if (congestedMode) {

            showCongestedState();
        }
    }

    // ============================================================
    // WINDOW
    // ============================================================

    private void configureWindow() {

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setBackground(
                APP_BG
        );

        setLayout(
                new BorderLayout()
        );

        setMinimumSize(
                new Dimension(
                        1180,
                        760
                )
        );

        setSize(
                1380,
                880
        );

        setLocationRelativeTo(
                null
        );
    }

    // ============================================================
    // ROOT
    // ============================================================

    private void buildUI() {

        add(
                buildHeader(),
                BorderLayout.NORTH
        );

        add(
                buildContent(),
                BorderLayout.CENTER
        );
    }

    // ============================================================
    // HEADER
    // ============================================================

    private JComponent buildHeader() {

        JPanel header =
                new JPanel(
                        new BorderLayout(
                                14,
                                0
                        )
                );

        header.setBackground(
                TEAL_DARK
        );

        header.setBorder(
                new EmptyBorder(
                        15,
                        20,
                        15,
                        20
                )
        );

        // --------------------------------------------------------
        // Application title
        // --------------------------------------------------------

        JPanel titlePanel =
                new JPanel();

        titlePanel.setOpaque(false);

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel(
                        "SMART LAUNDRY FACILITY"
                );

        title.setForeground(
                Color.WHITE
        );

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        22
                )
        );

        JLabel subtitle =
                new JLabel(
                        "Live operations dashboard"
                );

        subtitle.setForeground(
                new Color(
                        204,
                        251,
                        241
                )
        );

        subtitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        12
                )
        );

        titlePanel.add(
                title
        );

        titlePanel.add(
                Box.createVerticalStrut(2)
        );

        titlePanel.add(
                subtitle
        );

        header.add(
                titlePanel,
                BorderLayout.WEST
        );

        // --------------------------------------------------------
        // Mode badges
        // --------------------------------------------------------

        JPanel center =
                new JPanel(
                        new FlowLayout(
                                FlowLayout.CENTER,
                                8,
                                4
                        )
                );

        center.setOpaque(false);

        modeLabel =
                makeHeaderBadge(
                        congestedMode
                                ? "CONGESTED"
                                : "NORMAL",
                        congestedMode
                                ? ORANGE
                                : GREEN
                );

        liveLabel =
                makeHeaderBadge(
                        "● LIVE",
                        GREEN
                );

        center.add(
                modeLabel
        );

        center.add(
                liveLabel
        );

        header.add(
                center,
                BorderLayout.CENTER
        );

        // --------------------------------------------------------
        // Simulation timer
        // --------------------------------------------------------

        JPanel clockPanel =
                new JPanel();

        clockPanel.setOpaque(false);

        clockPanel.setLayout(
                new BoxLayout(
                        clockPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel caption =
                new JLabel(
                        "SIMULATION TIME"
                );

        caption.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        caption.setForeground(
                new Color(
                        204,
                        251,
                        241
                )
        );

        caption.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        9
                )
        );

        timerLabel =
                new JLabel(
                        "00:00 / 01:00"
                );

        timerLabel.setAlignmentX(
                Component.RIGHT_ALIGNMENT
        );

        timerLabel.setForeground(
                Color.WHITE
        );

        timerLabel.setFont(
                new Font(
                        "Consolas",
                        Font.BOLD,
                        17
                )
        );

        clockPanel.add(
                caption
        );

        clockPanel.add(
                Box.createVerticalStrut(2)
        );

        clockPanel.add(
                timerLabel
        );

        header.add(
                clockPanel,
                BorderLayout.EAST
        );

        return header;
    }

    private JLabel makeHeaderBadge(
            String text,
            Color color
    ) {

        JLabel label =
                new JLabel(
                        text
                );

        label.setOpaque(true);

        label.setBackground(
                color
        );

        label.setForeground(
                Color.WHITE
        );

        label.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        10
                )
        );

        label.setBorder(
                new EmptyBorder(
                        6,
                        10,
                        6,
                        10
                )
        );

        return label;
    }

    // ============================================================
    // CONTENT
    // ============================================================

    private JComponent buildContent() {

        JPanel root =
                new JPanel(
                        new BorderLayout(
                                12,
                                12
                        )
                );

        root.setBackground(
                APP_BG
        );

        root.setBorder(
                new EmptyBorder(
                        12,
                        14,
                        12,
                        14
                )
        );

        root.add(
                buildMetricsRow(),
                BorderLayout.NORTH
        );

        JSplitPane split =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        buildOperationsPanel(),
                        buildActivityPanel()
                );

        split.setResizeWeight(
                0.70
        );

        split.setDividerSize(
                6
        );

        split.setBorder(
                null
        );

        split.setOpaque(
                false
        );

        root.add(
                split,
                BorderLayout.CENTER
        );

        root.add(
                buildBottomPanel(),
                BorderLayout.SOUTH
        );

        return root;
    }

    // ============================================================
    // METRICS
    // ============================================================

    private JPanel buildMetricsRow() {

        JPanel row =
                new JPanel(
                        new GridLayout(
                                1,
                                4,
                                10,
                                0
                        )
                );

        row.setOpaque(false);

        row.setPreferredSize(
                new Dimension(
                        0,
                        76
                )
        );

        arrivedValue =
                new JLabel(
                        "0 / "
                                + ConfigLoader
                                .getCustomerCount()
                );

        servedValue =
                new JLabel(
                        "0"
                );

        averageValue =
                new JLabel(
                        "0.0 s"
                );

        activeValue =
                new JLabel(
                        "0"
                );

        row.add(
                metricCard(
                        "CUSTOMERS ADMITTED",
                        arrivedValue,
                        TEAL,
                        SOFT_TEAL
                )
        );

        row.add(
                metricCard(
                        "CUSTOMERS SERVED",
                        servedValue,
                        GREEN,
                        SOFT_GREEN
                )
        );

        row.add(
                metricCard(
                        "AVERAGE TIME",
                        averageValue,
                        PURPLE,
                        SOFT_PURPLE
                )
        );

        row.add(
                metricCard(
                        "ACTIVE CUSTOMERS",
                        activeValue,
                        ORANGE,
                        SOFT_ORANGE
                )
        );

        return row;
    }

    private JPanel metricCard(
            String caption,
            JLabel value,
            Color accent,
            Color soft
    ) {

        JPanel card =
                new JPanel(
                        new BorderLayout(
                                10,
                                0
                        )
                );

        card.setBackground(
                WHITE
        );

        card.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(
                                9,
                                12,
                                9,
                                12
                        )
                )
        );

        JPanel bar =
                new JPanel();

        bar.setPreferredSize(
                new Dimension(
                        4,
                        1
                )
        );

        bar.setBackground(
                accent
        );

        JPanel text =
                new JPanel(
                        new BorderLayout(
                                0,
                                3
                        )
                );

        text.setOpaque(false);

        JLabel small =
                new JLabel(
                        caption
                );

        small.setForeground(
                MUTED
        );

        small.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        9
                )
        );

        value.setForeground(
                TEXT
        );

        value.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        20
                )
        );

        text.add(
                small,
                BorderLayout.NORTH
        );

        text.add(
                value,
                BorderLayout.CENTER
        );

        JPanel marker =
                new JPanel(
                        new GridBagLayout()
                );

        marker.setPreferredSize(
                new Dimension(
                        30,
                        30
                )
        );

        marker.setBackground(
                soft
        );

        JLabel dot =
                new JLabel(
                        "•"
                );

        dot.setForeground(
                accent
        );

        dot.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        21
                )
        );

        marker.add(
                dot
        );

        card.add(
                bar,
                BorderLayout.WEST
        );

        card.add(
                text,
                BorderLayout.CENTER
        );

        card.add(
                marker,
                BorderLayout.EAST
        );

        return card;
    }

    // ============================================================
    // OPERATIONS
    // ============================================================

    private JPanel buildOperationsPanel() {

        JPanel root =
                new JPanel();

        root.setOpaque(false);

        root.setLayout(
                new BoxLayout(
                        root,
                        BoxLayout.Y_AXIS
                )
        );

        root.add(
                resourceSection(
                        "WASHING",
                        "6 machines",
                        MachineType.WASHER,
                        ConfigLoader.getWasherCount(),
                        "WASHER-"
                )
        );

        root.add(
                Box.createVerticalStrut(
                        9
                )
        );

        root.add(
                resourceSection(
                        "DRYING",
                        "4 machines",
                        MachineType.DRYER,
                        ConfigLoader.getDryerCount(),
                        "DRYER-"
                )
        );

        root.add(
                Box.createVerticalStrut(
                        9
                )
        );

        root.add(
                resourceSection(
                        "PAYMENT",
                        "2 kiosks",
                        MachineType.KIOSK,
                        ConfigLoader.getKioskCount(),
                        "KIOSK-"
                )
        );

        root.add(
                Box.createVerticalStrut(
                        9
                )
        );

        root.add(
                buildSafetyStrip()
        );

        return root;
    }

    private JPanel resourceSection(
            String title,
            String countText,
            MachineType type,
            int count,
            String prefix
    ) {

        JPanel wrapper =
                new JPanel(
                        new BorderLayout(
                                0,
                                8
                        )
                );

        wrapper.setBackground(
                WHITE
        );

        wrapper.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(
                                9,
                                10,
                                10,
                                10
                        )
                )
        );

        JPanel heading =
                new JPanel(
                        new BorderLayout()
                );

        heading.setOpaque(false);

        JLabel left =
                new JLabel(
                        title
                );

        left.setForeground(
                TEXT
        );

        left.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        11
                )
        );

        JLabel right =
                new JLabel(
                        countText
                );

        right.setForeground(
                MUTED
        );

        right.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        10
                )
        );

        heading.add(
                left,
                BorderLayout.WEST
        );

        heading.add(
                right,
                BorderLayout.EAST
        );

        wrapper.add(
                heading,
                BorderLayout.NORTH
        );

        JPanel grid =
                new JPanel(
                        new GridLayout(
                                1,
                                count,
                                8,
                                0
                        )
                );

        grid.setOpaque(false);

        for (
                int i = 1;
                i <= count;
                i++
        ) {

            MachineTile.TileType tileType =
                    MachineTile.TileType.valueOf(
                            type.name()
                    );

            MachineTile tile =
                    new MachineTile(
                            prefix + i,
                            tileType
                    );

            tileMap.put(
                    type.name()
                            + "-"
                            + i,
                    tile
            );

            grid.add(
                    tile
            );
        }

        wrapper.add(
                grid,
                BorderLayout.CENTER
        );

        return wrapper;
    }

    // ============================================================
    // SAFETY STRIP
    // ============================================================

    private JPanel buildSafetyStrip() {

        JPanel strip =
                new JPanel(
                        new GridLayout(
                                1,
                                4,
                                8,
                                0
                        )
                );

        strip.setBackground(
                WHITE
        );

        strip.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(
                                8,
                                10,
                                8,
                                10
                        )
                )
        );

        washerPeakValue =
                safetyValue(
                        "WASHER PEAK",
                        "0 / "
                                + ConfigLoader
                                .getWasherCount(),
                        TEAL
                );

        dryerPeakValue =
                safetyValue(
                        "DRYER PEAK",
                        "0 / "
                                + ConfigLoader
                                .getDryerCount(),
                        BLUE
                );

        washerFailureValue =
                safetyValue(
                        "WASHER FAILURES",
                        "0",
                        RED
                );

        paymentFailureValue =
                safetyValue(
                        "PAYMENT FAILURES",
                        "0",
                        ORANGE
                );

        strip.add(
                washerPeakValue.getParent()
        );

        strip.add(
                dryerPeakValue.getParent()
        );

        strip.add(
                washerFailureValue.getParent()
        );

        strip.add(
                paymentFailureValue.getParent()
        );

        return strip;
    }

    private JLabel safetyValue(
            String title,
            String valueText,
            Color accent
    ) {

        JPanel item =
                new JPanel(
                        new BorderLayout(
                                7,
                                0
                        )
                );

        item.setOpaque(false);

        JPanel line =
                new JPanel();

        line.setPreferredSize(
                new Dimension(
                        4,
                        1
                )
        );

        line.setBackground(
                accent
        );

        JPanel labels =
                new JPanel(
                        new BorderLayout(
                                0,
                                2
                        )
                );

        labels.setOpaque(false);

        JLabel titleLabel =
                new JLabel(
                        title
                );

        titleLabel.setForeground(
                MUTED
        );

        titleLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        8
                )
        );

        JLabel value =
                new JLabel(
                        valueText
                );

        value.setForeground(
                TEXT
        );

        value.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        11
                )
        );

        labels.add(
                titleLabel,
                BorderLayout.NORTH
        );

        labels.add(
                value,
                BorderLayout.CENTER
        );

        item.add(
                line,
                BorderLayout.WEST
        );

        item.add(
                labels,
                BorderLayout.CENTER
        );

        return value;
    }

    // ============================================================
    // ACTIVITY
    // ============================================================

    private JComponent buildActivityPanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                0,
                                8
                        )
                );

        panel.setBackground(
                WHITE
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(
                                10,
                                10,
                                10,
                                10
                        )
                )
        );

        JPanel top =
                new JPanel(
                        new BorderLayout()
                );

        top.setOpaque(false);

        JPanel titleBox =
                new JPanel();

        titleBox.setOpaque(false);

        titleBox.setLayout(
                new BoxLayout(
                        titleBox,
                        BoxLayout.Y_AXIS
                )
        );

        JLabel title =
                new JLabel(
                        "LIVE ACTIVITY"
                );

        title.setForeground(
                TEXT
        );

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        JLabel subtitle =
                new JLabel(
                        "Thread-labelled simulation events"
                );

        subtitle.setForeground(
                MUTED
        );

        subtitle.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        10
                )
        );

        titleBox.add(
                title
        );

        titleBox.add(
                Box.createVerticalStrut(
                        1
                )
        );

        titleBox.add(
                subtitle
        );

        JButton clear =
                new JButton(
                        "Clear"
                );

        clear.setFocusPainted(
                false
        );

        clear.setFont(
                new Font(
                        "Segoe UI",
                        Font.PLAIN,
                        10
                )
        );

        clear.setMargin(
                new Insets(
                        3,
                        9,
                        3,
                        9
                )
        );

        clear.addActionListener(
                e ->
                        activityArea.setText(
                                ""
                        )
        );

        top.add(
                titleBox,
                BorderLayout.WEST
        );

        top.add(
                clear,
                BorderLayout.EAST
        );

        panel.add(
                top,
                BorderLayout.NORTH
        );

        activityArea =
                new JTextArea();

        activityArea.setEditable(
                false
        );

        activityArea.setLineWrap(
                false
        );

        activityArea.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        11
                )
        );

        activityArea.setForeground(
                new Color(
                        30,
                        41,
                        59
                )
        );

        activityArea.setBackground(
                new Color(
                        248,
                        250,
                        252
                )
        );

        activityArea.setMargin(
                new Insets(
                        9,
                        9,
                        9,
                        9
                )
        );

        JScrollPane scroll =
                new JScrollPane(
                        activityArea
                );

        scroll.setBorder(
                new LineBorder(
                        BORDER,
                        1
                )
        );

        scroll.getViewport().setBackground(
                activityArea.getBackground()
        );

        panel.add(
                scroll,
                BorderLayout.CENTER
        );

        return panel;
    }

    // ============================================================
    // BOTTOM QUEUES + LARGE ALERT
    // ============================================================

    private JPanel buildBottomPanel() {

        /*
         * Vertical layout:
         *
         * Row 1 = three waiting queues
         * Row 2 = large centred alert
         *
         * The alert is no longer squeezed onto the right side.
         */
        JPanel root =
                new JPanel(
                        new BorderLayout(
                                0,
                                8
                        )
                );

        root.setOpaque(false);

        // ========================================================
        // WAITING QUEUES
        // ========================================================

        JPanel queuePanel =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                8,
                                0
                        )
                );

        queuePanel.setBackground(
                WHITE
        );

        queuePanel.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                BORDER,
                                1,
                                true
                        ),
                        new EmptyBorder(
                                8,
                                10,
                                8,
                                10
                        )
                )
        );

        washerQueueValue =
                queueValue(
                        "WASH QUEUE",
                        TEAL
                );

        dryerQueueValue =
                queueValue(
                        "DRY QUEUE",
                        BLUE
                );

        paymentQueueValue =
                queueValue(
                        "PAYMENT QUEUE",
                        ORANGE
                );

        queuePanel.add(
                washerQueueValue.getParent()
        );

        queuePanel.add(
                dryerQueueValue.getParent()
        );

        queuePanel.add(
                paymentQueueValue.getParent()
        );

        root.add(
                queuePanel,
                BorderLayout.NORTH
        );

        // ========================================================
        // LARGE CENTRED ALERT
        // ========================================================

        alertBanner =
                new JLabel(
                        "",
                        SwingConstants.CENTER
                );

        alertBanner.setOpaque(
                true
        );

        alertBanner.setBackground(
                RED
        );

        alertBanner.setForeground(
                Color.WHITE
        );

        /*
         * Large, easy-to-read alert text.
         */
        alertBanner.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        15
                )
        );

        /*
         * Explicit horizontal and vertical centering.
         */
        alertBanner.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        alertBanner.setVerticalAlignment(
                SwingConstants.CENTER
        );

        /*
         * Give the alert enough height for two lines.
         */
        alertBanner.setPreferredSize(
                new Dimension(
                        0,
                        64
                )
        );

        alertBanner.setMinimumSize(
                new Dimension(
                        0,
                        64
                )
        );

        /*
         * Rounded border and comfortable internal spacing.
         */
        alertBanner.setBorder(
                BorderFactory.createCompoundBorder(
                        new LineBorder(
                                new Color(
                                        185,
                                        28,
                                        28
                                ),
                                1,
                                true
                        ),
                        new EmptyBorder(
                                10,
                                20,
                                10,
                                20
                        )
                )
        );

        /*
         * Start hidden during normal mode.
         */
        alertBanner.setVisible(
                false
        );

        /*
         * CENTER makes the alert span the available width
         * instead of being restricted to a small EAST panel.
         */
        root.add(
                alertBanner,
                BorderLayout.CENTER
        );

        return root;
    }

    // ============================================================
    // QUEUE VALUE
    // ============================================================

    private JLabel queueValue(
            String title,
            Color accent
    ) {

        JPanel item =
                new JPanel(
                        new BorderLayout(
                                0,
                                1
                        )
                );

        item.setBackground(
                new Color(
                        248,
                        250,
                        252
                )
        );

        item.setBorder(
                new LineBorder(
                        BORDER,
                        1,
                        true
                )
        );

        JLabel caption =
                new JLabel(
                        title,
                        SwingConstants.CENTER
                );

        caption.setForeground(
                MUTED
        );

        caption.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        8
                )
        );

        JLabel value =
                new JLabel(
                        "0",
                        SwingConstants.CENTER
                );

        value.setForeground(
                accent
        );

        value.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        17
                )
        );

        item.add(
                caption,
                BorderLayout.NORTH
        );

        item.add(
                value,
                BorderLayout.CENTER
        );

        return value;
    }

    // ============================================================
    // TIMER
    // ============================================================

    private void installTimers() {

        statTimer =
                new Timer(
                        250,
                        e ->
                                refreshStats()
                );

        statTimer.start();

        clockTimer =
                new Timer(
                        1000,
                        e ->
                                updateClock()
                );

        clockTimer.start();
    }

    private void updateClock() {

        long elapsed =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - startTime
                ) / 1000L;

        long configured =
                ConfigLoader
                        .getSimulationSeconds();

        timerLabel.setText(
                String.format(
                        "%02d:%02d / %02d:%02d",
                        elapsed / 60,
                        elapsed % 60,
                        configured / 60,
                        configured % 60
                )
        );
    }

    // ============================================================
    // CONGESTED MODE
    // ============================================================

    private void showCongestedState() {

        /*
         * Initial warning before the owner-call threshold.
         */
        alertBanner.setText(
                "<html>"
                        + "<div style='text-align:center;'>"
                        + "<b>PAYMENT KIOSKS UNAVAILABLE</b>"
                        + "</div>"
                        + "</html>"
        );

        alertBanner.setBackground(
                ORANGE
        );

        alertBanner.setVisible(
                true
        );

        for (
                int i = 1;
                i <= ConfigLoader.getKioskCount();
                i++
        ) {

            MachineTile tile =
                    tileMap.get(
                            "KIOSK-" + i
                    );

            if (tile != null) {

                tile.setOutOfOrder();
            }
        }

        appendLog(
                "SYSTEM | Both payment kiosks are out of order."
        );
    }

    // ============================================================
    // STATISTICS
    // ============================================================

    public void refreshStats() {

        int arrived =
                stats.getArrived();

        int served =
                stats.getServed();

        int abandoned =
                stats.getAbandoned();

        int active =
                Math.max(
                        0,
                        arrived
                                - served
                                - abandoned
                );

        arrivedValue.setText(
                arrived
                        + " / "
                        + ConfigLoader
                        .getCustomerCount()
        );

        servedValue.setText(
                String.valueOf(
                        served
                )
        );

        averageValue.setText(
                String.format(
                        "%.1f s",
                        stats.getAverageTime()
                )
        );

        activeValue.setText(
                String.valueOf(
                        active
                )
        );

        // --------------------------------------------------------
        // Waiting queues
        // --------------------------------------------------------

        washerQueueValue.setText(
                String.valueOf(
                        resources
                                .getWasherWaitCount()
                )
        );

        dryerQueueValue.setText(
                String.valueOf(
                        resources
                                .getDryerWaitCount()
                )
        );

        paymentQueueValue.setText(
                String.valueOf(
                        resources
                                .getKioskWaitCount()
                )
        );

        // --------------------------------------------------------
        // Resource peaks
        // --------------------------------------------------------

        washerPeakValue.setText(
                stats.getMaxWashers()
                        + " / "
                        + ConfigLoader
                        .getWasherCount()
        );

        dryerPeakValue.setText(
                stats.getMaxDryers()
                        + " / "
                        + ConfigLoader
                        .getDryerCount()
        );

        // --------------------------------------------------------
        // Failure statistics
        // --------------------------------------------------------

        washerFailureValue.setText(
                String.valueOf(
                        stats.getWasherFailures()
                )
        );

        paymentFailureValue.setText(
                String.valueOf(
                        stats.getPaymentFailures()
                )
        );

        // --------------------------------------------------------
        // Simulation completed
        // --------------------------------------------------------

        if (
                arrived > 0
                        && served + abandoned >= arrived
                        && !finished
        ) {

            finished = true;

            liveLabel.setText(
                    "● COMPLETE"
            );

            liveLabel.setBackground(
                    TEAL
            );
        }

        // --------------------------------------------------------
        // Owner-call alert
        // --------------------------------------------------------

        if (
                congestedMode
                        && resources.isOwnerCalled()
        ) {

            alertBanner.setText(
                    "<html>"
                            + "<div style='text-align:center;'>"
                            + "<b>OWNER CALLED</b>"
                            + "<br>"
                            + "PAYMENT QUEUE REACHED 30"
                            + "</div>"
                            + "</html>"
            );

            alertBanner.setFont(
                    new Font(
                            "Segoe UI",
                            Font.BOLD,
                            15
                    )
            );

            alertBanner.setBackground(
                    RED
            );

            alertBanner.setForeground(
                    Color.WHITE
            );

            alertBanner.setVisible(
                    true
            );
        }
    }

    // ============================================================
    // MACHINE BUSY
    // ============================================================

    @Override
    public void onMachineBusy(
            MachineType type,
            int machineId,
            int customerId,
            long durationMillis
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    MachineTile tile =
                            tileMap.get(
                                    type.name()
                                            + "-"
                                            + machineId
                            );

                    if (tile != null) {

                        tile.setBusy(
                                customerId
                        );
                    }

                    appendLogNow(
                            type.name()
                                    + "-"
                                    + machineId
                                    + "  BUSY  <- CUSTOMER "
                                    + customerId
                                    + "  ("
                                    + durationMillis
                                    + " ms)"
                    );
                }
        );
    }

    // ============================================================
    // MACHINE IDLE
    // ============================================================

    @Override
    public void onMachineIdle(
            MachineType type,
            int machineId
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    MachineTile tile =
                            tileMap.get(
                                    type.name()
                                            + "-"
                                            + machineId
                            );

                    if (tile != null) {

                        if (
                                congestedMode
                                        && type
                                        == MachineType.KIOSK
                        ) {

                            tile.setOutOfOrder();

                        } else {

                            tile.setIdle();
                        }
                    }

                    appendLogNow(
                            type.name()
                                    + "-"
                                    + machineId
                                    + "  IDLE"
                    );
                }
        );
    }

    // ============================================================
    // MACHINE FAILURE
    // ============================================================

    @Override
    public void onMachineFailed(
            MachineType type,
            int machineId,
            int customerId
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    MachineTile tile =
                            tileMap.get(
                                    type.name()
                                            + "-"
                                            + machineId
                            );

                    if (tile != null) {

                        if (
                                congestedMode
                                        && type
                                        == MachineType.KIOSK
                        ) {

                            tile.setOutOfOrder();

                        } else {

                            tile.setFailed(
                                    customerId
                            );
                        }
                    }

                    appendLogNow(
                            "FAILURE  "
                                    + type.name()
                                    + "-"
                                    + machineId
                                    + "  <- CUSTOMER "
                                    + customerId
                    );
                }
        );
    }

    // ============================================================
    // CONGESTION START
    // ============================================================

    @Override
    public void onCongestionStarted() {

        SwingUtilities.invokeLater(
                () -> {

                    alertBanner.setText(
                            "<html>"
                                    + "<div style='text-align:center;'>"
                                    + "<b>OWNER CALLED</b>"
                                    + "<br>"
                                    + "PAYMENT QUEUE REACHED 30"
                                    + "</div>"
                                    + "</html>"
                    );

                    alertBanner.setFont(
                            new Font(
                                    "Segoe UI",
                                    Font.BOLD,
                                    15
                            )
                    );

                    alertBanner.setForeground(
                            Color.WHITE
                    );

                    alertBanner.setBackground(
                            RED
                    );

                    alertBanner.setVisible(
                            true
                    );

                    appendLogNow(
                            "CONGESTION | 30 waiting customers | OWNER CALLED"
                    );
                }
        );
    }

    // ============================================================
    // CONGESTION RESOLVED
    // ============================================================

    @Override
    public void onCongestionResolved() {

        SwingUtilities.invokeLater(
                () -> {

                    if (!congestedMode) {

                        alertBanner.setVisible(
                                false
                        );
                    }

                    appendLogNow(
                            "CONGESTION RESOLVED"
                    );
                }
        );
    }

    // ============================================================
    // LOGGING
    // ============================================================

    public void appendLog(
            String message
    ) {

        SwingUtilities.invokeLater(
                () ->
                        appendLogNow(
                                message
                        )
        );
    }

    private void appendLogNow(
            String message
    ) {

        activityArea.append(
                "["
                        + TimeUtil.timestamp()
                        + "] "
                        + message
                        + "\n"
        );

        activityArea.setCaretPosition(
                activityArea
                        .getDocument()
                        .getLength()
        );
    }

    // ============================================================
    // CLEANUP
    // ============================================================

    @Override
    public void dispose() {

        if (statTimer != null) {

            statTimer.stop();
        }

        if (clockTimer != null) {

            clockTimer.stop();
        }

        super.dispose();
    }
}