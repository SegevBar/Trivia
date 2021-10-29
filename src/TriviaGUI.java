package il.ac.tau.cs.sw1.trivia;

import java.io.*;
import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TriviaGUI {

	private static final int MAX_ERRORS = 3;
	private Shell shell;
	private Label scoreLabel;
	private Composite questionPanel;
	private Label startupMessageLabel;
	private Font boldFont;
	private String lastAnswer;

	private String path;
	private List<List<String>> questionBank;
	private List<List<String>> usedQuestions;
	private int score;
	private int index;
	private int counter;
	private String curAnswer;
	private int errorCount;
	private boolean neverPressedPass;
	private boolean neverPressedfirstfifty;
	
	// Currently visible UI elements.
	Label instructionLabel;
	Label questionLabel;
	private List<Button> answerButtons = new LinkedList<>();
	private Button passButton;
	private Button fiftyFiftyButton;

	public void open() {
		createShell();
		runApplication();
	}

	/**
	 * Creates the widgets of the application main window
	 */
	private void createShell() {
		Display display = Display.getDefault();
		shell = new Shell(display);
		shell.setText("Trivia");

		// window style
		Rectangle monitor_bounds = shell.getMonitor().getBounds();
		shell.setSize(new Point(monitor_bounds.width / 3,
				monitor_bounds.height / 4));
		shell.setLayout(new GridLayout());

		FontData fontData = new FontData();
		fontData.setStyle(SWT.BOLD);
		boldFont = new Font(shell.getDisplay(), fontData);

		// create window panels
		createFileLoadingPanel();
		createScorePanel();
		createQuestionPanel();
	}

	/**
	 * Creates the widgets of the form for trivia file selection
	 */
	private void createFileLoadingPanel() {
		final Composite fileSelection = new Composite(shell, SWT.NULL);
		fileSelection.setLayoutData(GUIUtils.createFillGridData(1));
		fileSelection.setLayout(new GridLayout(4, false));

		final Label label = new Label(fileSelection, SWT.NONE);
		label.setText("Enter trivia file path: ");

		// text field to enter the file path
		final Text filePathField = new Text(fileSelection, SWT.SINGLE
				| SWT.BORDER);
		filePathField.setLayoutData(GUIUtils.createFillGridData(1));

		// "Browse" button
		final Button browseButton = new Button(fileSelection, SWT.PUSH);
		browseButton.setText("Browse");
		browseButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.getSource() instanceof Button) {
					path = GUIUtils.getFilePathFromFileDialog(shell);
					if (path != null) {
						filePathField.setText(path);
					}
				}
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		// "Play!" button
		final Button playButton = new Button(fileSelection, SWT.PUSH);
		playButton.setText("Play!");
		playButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				questionBank = new ArrayList<List<String>>();
				usedQuestions = new ArrayList<List<String>>();
				score = 0;
				index = 0;
				counter = 0;
				curAnswer = "";
				errorCount = 0;
				neverPressedPass = true;
				neverPressedfirstfifty = true;
				scoreLabel.setText(Integer.toString(score));

				BufferedReader bufferedReader;
				String filePath = filePathField.getText();
				try {
					bufferedReader = new BufferedReader(new FileReader(new File(filePath)));
					String[] splitLine;
					String line;
					int row = 0;
					try {
						while ((line = bufferedReader.readLine()) != null) {
							row++;
							splitLine = line.split("\t");
							List<String> QNA = Arrays.asList(splitLine);
							if (QNA.size() != 5) {
								GUIUtils.showErrorDialog(shell, "Trivia file format error: Trivia file row must containg a question and four answers, seperated by tabs. (row " + row + ")");
								continue;
							}
							questionBank.add(QNA);
						}
						bufferedReader.close();

					} catch (IOException error) {
						GUIUtils.showErrorDialog(shell, "Trivia file format error: Trivia file row must containg a question and four answers, seperated by tabs. (row " + 1 + ")");
					}

				} catch (FileNotFoundException error) {
					GUIUtils.showErrorDialog(shell, "File " + filePath + " not found");
				}
				Collections.shuffle(questionBank);
				showQuestion();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e){}
		});
	}

	private void showQuestion() {
		List<String> curQustion = getQuestion();
		if (curQustion != null) {

			curAnswer = curQustion.get(1);
			String question = curQustion.get(0);
			List<String> answers = curQustion.subList(1,5);
			Collections.shuffle(answers);
			updateQuestionPanel(question, answers);
			updateClueButtons();
		} else {
			GUIUtils.showInfoDialog(shell, "GAME OVER", "Your final score is " + score + " after " + counter + " questions.");
			for (Button button : answerButtons) {
				button.setEnabled(false);
			}
			passButton.setEnabled(false);
			fiftyFiftyButton.setEnabled(false);
		}
	}

	private List<String> getQuestion() {
		while (index < questionBank.size()) {
			boolean inUsed = false;
			List<String> curQustion = questionBank.get(index);
			for (List<String> set : usedQuestions) {
				if (sameSet(curQustion, set)) {
					inUsed = true;
				}
			}
			if (inUsed) {
				index++;
			} else {
				usedQuestions.add(curQustion);
				index++;
				return curQustion;
			}
		}
		return null;
	}

	private boolean sameSet(List<String> set1, List<String> set2) {
		String q1 = set1.get(0);
		String q2 = set2.get(0);
		if (q1.equals(q2)) {
			List<String> ans1 = set1.subList(1,5);
			List<String> ans2 = set2.subList(1,5);
			Collections.sort(ans1, Comparator.naturalOrder());
			Collections.sort(ans2, Comparator.naturalOrder());
			for (int i=0; i<4; i++) {
				if (!(ans1.get(i).equals(ans2.get(i)))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private void updateClueButtons() {
		if (score > 0 || neverPressedPass) {
			passButton.setEnabled(true);
		} else {
			passButton.setEnabled(false);
		}

		if (score > 0 || neverPressedfirstfifty) {
			fiftyFiftyButton.setEnabled(true);
		} else {
			fiftyFiftyButton.setEnabled(false);
		}
	}

	/**
	 * Creates the panel that displays the current score
	 */
	private void createScorePanel() {
		Composite scorePanel = new Composite(shell, SWT.BORDER);
		scorePanel.setLayoutData(GUIUtils.createFillGridData(1));
		scorePanel.setLayout(new GridLayout(2, false));

		final Label label = new Label(scorePanel, SWT.NONE);
		label.setText("Total score: ");

		// The label which displays the score; initially empty
		scoreLabel = new Label(scorePanel, SWT.NONE);
		scoreLabel.setLayoutData(GUIUtils.createFillGridData(1));
	}

	/**
	 * Creates the panel that displays the questions, as soon as the game
	 * starts. See the updateQuestionPanel for creating the question and answer
	 * buttons
	 */
	private void createQuestionPanel() {
		questionPanel = new Composite(shell, SWT.BORDER);
		questionPanel.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));
		questionPanel.setLayout(new GridLayout(2, true));

		// Initially, only displays a message
		startupMessageLabel = new Label(questionPanel, SWT.NONE);
		startupMessageLabel.setText("No question to display, yet.");
		startupMessageLabel.setLayoutData(GUIUtils.createFillGridData(2));
	}

	/**
	 * Serves to display the question and answer buttons
	 */
	private void updateQuestionPanel(String question, List<String> answers) {
		// Save current list of answers.
		List<String> currentAnswers = answers;
		
		// clear the question panel
		Control[] children = questionPanel.getChildren();
		for (Control control : children) {
			control.dispose();
		}

		// create the instruction label
		instructionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		instructionLabel.setText(lastAnswer + "Answer the following question:");
		instructionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the question label
		questionLabel = new Label(questionPanel, SWT.CENTER | SWT.WRAP);
		questionLabel.setText(question);
		questionLabel.setFont(boldFont);
		questionLabel.setLayoutData(GUIUtils.createFillGridData(2));

		// create the answer buttons
		answerButtons.clear();
		for (int i = 0; i < 4; i++) {
			Button answerButton = new Button(questionPanel, SWT.PUSH | SWT.WRAP);
			answerButton.setText(answers.get(i));
			GridData answerLayoutData = GUIUtils.createFillGridData(1);
			answerLayoutData.verticalAlignment = SWT.FILL;
			answerButton.setLayoutData(answerLayoutData);

			answerButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (e.getSource() instanceof Button) {
						counter++;
						Button ans = (Button) e.getSource();
						if (curAnswer.equals(ans.getText())) {
							score += 3;
							errorCount = 0;
							lastAnswer = "Correct! ";
							scoreLabel.setText(Integer.toString(score));
						}
						else {
							score -= 2;
							errorCount++;
							lastAnswer = "Wrong! ";
							scoreLabel.setText(Integer.toString(score));
						}
						if (errorCount == MAX_ERRORS) {
							GUIUtils.showInfoDialog(shell, "GAME OVER", "Your final score is " + score + " after " + counter + " questions.");
							for (Button button : answerButtons) {
								button.setEnabled(false);
							}
							passButton.setEnabled(false);
							fiftyFiftyButton.setEnabled(false);
						} else {
							showQuestion();
						}
					}
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			answerButtons.add(answerButton);
		}

		// create the "Pass" button to skip a question
		passButton = new Button(questionPanel, SWT.PUSH);
		passButton.setText("Pass");
		GridData data = new GridData(GridData.END, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		passButton.setLayoutData(data);
		passButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (neverPressedPass) {
					neverPressedPass = false;
				}
				else {
					score = score-1;
					scoreLabel.setText(Integer.toString(score));
				}
				showQuestion();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		// create the "50-50" button to show fewer answer options
		fiftyFiftyButton = new Button(questionPanel, SWT.PUSH);
		fiftyFiftyButton.setText("50-50");
		data = new GridData(GridData.BEGINNING, GridData.CENTER, true,
				false);
		data.horizontalSpan = 1;
		fiftyFiftyButton.setLayoutData(data);
		fiftyFiftyButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				List<Button> half = new ArrayList<Button>();
				for (Button ans : answerButtons) {
					if (!(ans.getText().equals(curAnswer))) {
						half.add(ans);
					}
				}
				Collections.shuffle(half);
				half.get(0).setEnabled(false);
				half.get(1).setEnabled(false);

				if (neverPressedfirstfifty) {
					neverPressedfirstfifty = false;
					fiftyFiftyButton.setEnabled(false);
				} else {
					score = score-1;
					fiftyFiftyButton.setEnabled(false);
					scoreLabel.setText(Integer.toString(score));
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});


		// two operations to make the new widgets display properly
		questionPanel.pack();
		questionPanel.getParent().layout();
	}

	/**
	 * Opens the main window and executes the event loop of the application
	 */
	private void runApplication() {
		shell.open();
		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
		boldFont.dispose();
	}



}
