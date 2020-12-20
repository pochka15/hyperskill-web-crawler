package crawler;

import crawler.actions.Action;
import crawler.actions.CrawlHtmlPages;
import crawler.actions.InterruptibleAction;
import crawler.statemachine.guistatemachine.Event;
import crawler.statemachine.guistatemachine.State;
import crawler.statemachine.guistatemachine.StateMachine;
import crawler.statemachine.guistatemachine.Transition;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.function.Consumer;

import static crawler.statemachine.guistatemachine.Event.*;
import static crawler.statemachine.guistatemachine.State.*;
import static crawler.utilities.HtmlUtilities.parsedTitle;
import static crawler.utilities.IntegerUtilities.parseIntOrDefault;
import static crawler.utilities.WebCommunicationUtils.establishedCustomConnection;
import static crawler.utilities.WebCommunicationUtils.fetchedHtmlContent;

public class MainFrame extends JFrame {
    public static final String TABLE_COLUMN_0 = "URL";
    public static final String TABLE_COLUMN_1 = "Title";
    public final JLabel timeLimitUnitLabel;
    public final JLabel labelForTimeLimit;
    public final JTextField timeLimitTextField;
    public final JCheckBox timeLimitCheckBox;
    public final JTextField workersTextField;
    public final JLabel labelForWorkersTextField;
    public final DefaultTableModel tableModel;
    public final JTable titlesTable;
    public final JScrollPane tableScrollPane;
    public final JTextField urlTextField;
    public final JLabel titleLabel;
    public final JTextField exportUrlTextField;
    public final JButton exportButton;
    public final JTextField depthTextField;
    public final JCheckBox depthCheckBox;
    public final JLabel parsedPagesNumberLabel;
    public final JLabel labelForUrlTextField;
    public final JLabel labelForDepthTextField;
    public final JLabel labelForExportUrlTextField;
    public final JToggleButton runButton;
    public final JLabel labelForParsedPagesNumberLabel;

    private final StateMachine<State, Event> guiStateMachine;

    private CrawlConsumer currentCrawlConsumer;
    private InterruptibleAction crawlingAction;

    public MainFrame() {
        super();
        guiStateMachine = new StateMachine<>(
                INIT,
                Map.of(new Transition<>(INIT, START_CRAWLING, WAITING_TO_START_CRAWLING),
                       () -> {
                           var crawlParameters = collectedCrawlParameters();
                           if (crawlParameters.isPresent()) {
                               CrawlParameters parameters = crawlParameters.get();

                               onStartCrawling(parameters);

//                               Set and execute crawling action
                               crawlingAction = new CrawlHtmlPages(parameters.url,
                                                                   parameters.workersNumber,
                                                                   parameters.crawlDepth,
                                                                   currentCrawlConsumer
                               )
                                       .then(finishDisplayingAction());
                               crawlingAction.execute();
                           } else {
                               onFailedToStartCrawling();
                           }
                       },

                       new Transition<>(WAITING_TO_START_CRAWLING, FAILED_T0_START_CRAWLING, INIT),
                       () -> System.out.println("Failed to start crawling"),

                       new Transition<>(WAITING_TO_START_CRAWLING, CRAWLING_STARTED, DISPLAYING_CRAWLED_PAGES),
                       this::afterCrawlingStarted,

                       new Transition<>(DISPLAYING_CRAWLED_PAGES, FINISH_DISPLAYING, INIT),
                       this::deselectButton,

                       new Transition<>(DISPLAYING_CRAWLED_PAGES, INTERRUPT_CRAWLING, INIT),
                       this::onInterruptCrawling,

                       new Transition<>(INIT, EXPORT_CRAWLED_DATA, INIT),
                       this::onExportCrawledData,

                       new Transition<>(DISPLAYING_CRAWLED_PAGES, EXPORT_CRAWLED_DATA, DISPLAYING_CRAWLED_PAGES),
                       this::onExportCrawledData));
        labelForUrlTextField = added(new JLabel("Start URL:"));
        urlTextField = added(configured(new JTextField(55), c -> c.setName("UrlTextField")));
        runButton = added(configured(new JToggleButton("Run"), c -> {
            c.setName("RunButton");
            c.addActionListener(e -> {
                if (c.isSelected()) {
                    c.setEnabled(false);
                    guiStateMachine.handleEvent(START_CRAWLING);
                } else {
                    guiStateMachine.handleEvent(INTERRUPT_CRAWLING);
                }
            });
        }));
        labelForWorkersTextField = added(new JLabel("Workers number:"));
        workersTextField = added(new JTextField(60));
        labelForDepthTextField = added(new JLabel("Maximum depth:"));
        depthTextField = added(configured(new JTextField(50), c -> c.setName("DepthTextField")));
        depthCheckBox = added(configured(new JCheckBox("Enabled"), c -> {
            c.setName("DepthCheckBox");
            c.setSelected(true);
        }));
        labelForTimeLimit = added(new JLabel("Time limit:"));
        timeLimitTextField = added(new JTextField(40));
        timeLimitUnitLabel = added(new JLabel("seconds"));
        timeLimitCheckBox = added(configured(new JCheckBox("Enabled"), c -> c.setSelected(true)));
        labelForParsedPagesNumberLabel = added(new JLabel("Parsed pages number: "));
        parsedPagesNumberLabel
                = added(configured(new JLabel("0"), c -> c.setName("ParsedLabel")));
        labelForExportUrlTextField = added(new JLabel("Export url:"));
        exportUrlTextField = added(configured(new JTextField(60), c -> c.setName("ExportUrlTextField")));
        exportButton = added(configured(new JButton("Save"), c -> {
            c.setName("ExportButton");
            c.addActionListener(e -> guiStateMachine.handleEvent(EXPORT_CRAWLED_DATA));
        }));
        tableModel = configured(new DefaultTableModel(),
                                tableModel1 -> tableModel1.setColumnIdentifiers(new String[]{TABLE_COLUMN_0, TABLE_COLUMN_1}));
        titleLabel = added(configured(new JLabel(), l -> setName("TitleLabel")));
        titlesTable = configured(new JTable(tableModel), table -> {
            table.setEnabled(false);
            table.setName("TitlesTable");
        });
        tableScrollPane = added(new JScrollPane(titlesTable));

        configureFrame();
    }

    private void onFailedToStartCrawling() {
        guiStateMachine.handleEvent(FAILED_T0_START_CRAWLING);
    }

    private InterruptibleAction finishDisplayingAction() {
        return new InterruptibleAction() {
            boolean isInterrupted = false;

            @Override
            public void execute() {
                if (!isInterrupted) {
                    guiStateMachine.handleEvent(FINISH_DISPLAYING);
                }
            }

            @Override
            public void interrupt() {
                isInterrupted = true;
            }
        };
    }

    private Optional<CrawlParameters> collectedCrawlParameters() {
        URL url = null;
        int workersNumber = parseIntOrDefault(workersTextField.getText(), 10);
        int crawlDepth = 0;
        int timeLimit = timeLimitCheckBox.isSelected() ?
                parseIntOrDefault(timeLimitTextField.getText(), Integer.MAX_VALUE) :
                Integer.MAX_VALUE;
        try {
            url = new URL(urlTextField.getText());
            if (depthCheckBox.isSelected()) {
                crawlDepth = parseIntOrDefault(depthTextField.getText(), Integer.MAX_VALUE);
            }
        } catch (MalformedURLException e) {
            System.out.println("Incorrect url entered");
            e.printStackTrace();
        }
        if (url != null && crawlDepth >= 0 && workersNumber >= 0 && timeLimit >= 0) {
            return Optional.of(new CrawlParameters(url, workersNumber, crawlDepth, timeLimit));
        }
        return Optional.empty();
    }

    private synchronized void afterCrawlingStarted() {
        runButton.setEnabled(true);
    }

    private synchronized void onExportCrawledData() {
        String exportUrl = exportUrlTextField.getText();

        try (var bw = new BufferedWriter(new FileWriter(exportUrl))) {
            final var iterator = tableModel.getDataVector().iterator();
            while (iterator.hasNext()) {
                var vector = iterator.next();
                final String odd = vector.elementAt(0).toString();
                final String even = vector.elementAt(1).toString();
                bw.write(odd);
                bw.newLine();
                bw.write(even);
                if (iterator.hasNext())
                    bw.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private synchronized void deselectButton() {
        runButton.setSelected(false);
    }

    private synchronized void onInterruptCrawling() {
        deselectButton();
        currentCrawlConsumer.stopConsuming();
        crawlingAction.interrupt();
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 800);
        setLocationRelativeTo(null);
        setTitle("Web crawler");
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setVisible(true);
    }

    private <T> T configured(T component, Configuration<T> configuration) {
        configuration.executeFor(component);
        return component;
    }

    private <T extends JComponent> T added(T component) {
        this.add(component);
        return component;
    }

    private void onStartCrawling(CrawlParameters crawlParameters) {
        try {
//                Update title
            this.titleLabel.setText(parsedTitle(fetchedHtmlContent(establishedCustomConnection(crawlParameters.url))));
        } catch (IOException e) {
            e.printStackTrace();
        }
//            Update tableModel
        this.tableModel.setDataVector(new Vector<>(), new Vector<>(java.util.List.of(
                MainFrame.TABLE_COLUMN_0, MainFrame.TABLE_COLUMN_1)));

//            Reset number of parsed pages
        parsedPagesNumberLabel.setText("0");

//            Set consumer
        currentCrawlConsumer = new CrawlConsumer(
                guiStateMachine, tableModel, parsedPagesNumberLabel,
                new TimeChecker(crawlParameters.timeLimitInSeconds,
                                () -> guiStateMachine.handleEvent(INTERRUPT_CRAWLING)));
    }
}

@FunctionalInterface
interface Configuration<T> {
    void executeFor(T component);
}

class UrlWithPageTitle {
    public final URL url;
    public final String pageTitle;

    public UrlWithPageTitle(URL url, String pageTitle) {
        this.url = url;
        this.pageTitle = pageTitle;
    }
}

class CrawlConsumer implements Consumer<HtmlContentWithUrl> {
    private final StateMachine<State, Event> guiStateMachine;
    private final DefaultTableModel tableModel;
    private final JLabel parsedPagesNumberLabel;
    private final TimeChecker timeChecker;

    private boolean stopConsuming = false;
    private boolean noResultsWereDisplayedYet = true;

    public CrawlConsumer(StateMachine<State, Event> guiStateMachine,
                         DefaultTableModel tableModel,
                         JLabel parsedPagesNumberLabel,
                         TimeChecker timeChecker) {
        this.guiStateMachine = guiStateMachine;
        this.tableModel = tableModel;
        this.parsedPagesNumberLabel = parsedPagesNumberLabel;
        this.timeChecker = timeChecker;
    }

    @Override
    public void accept(HtmlContentWithUrl htmlContentWithUrl) {
        synchronized (this) {
            if (!stopConsuming) {
//                Update gui
                var urlWithPageTitle = new UrlWithPageTitle(htmlContentWithUrl.url,
                                                            parsedTitle(htmlContentWithUrl.htmlContent));
                tableModel.addRow(new Vector<>(java.util.List.of(
                        urlWithPageTitle.url.toString(), urlWithPageTitle.pageTitle)));
                final String numberAsString = parsedPagesNumberLabel.getText();
                final int crawledPagesNumber = parseIntOrDefault(numberAsString, 0) + 1;
                parsedPagesNumberLabel.setText(String.valueOf(crawledPagesNumber));
//                Send CRAWLING_STARTED if it wasn't sent
                if (noResultsWereDisplayedYet) {
                    noResultsWereDisplayedYet = false;
                    guiStateMachine.handleEvent(CRAWLING_STARTED);
                }
//                Run timer which will interrupt crawling if the time is up
                timeChecker.execute();
            }
        }
    }

    public void stopConsuming() {
        stopConsuming = true;
    }
}

class CrawlParameters {
    final URL url;
    final int workersNumber;
    final int crawlDepth;
    final int timeLimitInSeconds;

    public CrawlParameters(URL url, int workersNumber, int crawlDepth, int timeLimitInSeconds) {
        this.url = url;
        this.workersNumber = workersNumber;
        this.crawlDepth = crawlDepth;
        this.timeLimitInSeconds = timeLimitInSeconds;
    }
}

class TimeChecker implements Action {
    private final int timeLimitInSeconds;
    private final Action onTimeIsUp;
    long startTime = -1;


    public TimeChecker(int timeLimitInSeconds, Action onTimeIsUp) {
        this.timeLimitInSeconds = timeLimitInSeconds;
        this.onTimeIsUp = onTimeIsUp;
    }

    @Override
    public void execute() {
        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }
        if ((System.currentTimeMillis() - startTime) * 0.001 > timeLimitInSeconds) {
            onTimeIsUp.execute();
        }
    }
}