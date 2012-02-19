package net.nerdcamp.jaybee.serienjunkiesDownloader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Main {

	private static Display display = null;

	public static Display getDisplay() {
		if (Main.display == null) {
			Main.display = new Display();
		}
		return Main.display;
	}

	public static GridData constructGridData(final int horizontalSpan,
			final int verticalSpan, final int alignment, final boolean grabSpace) {
		return Main.constructGridData(horizontalSpan, verticalSpan, alignment,
				grabSpace, grabSpace);
	}

	public static GridData constructGridData(final int horizontalSpan,
			final int verticalSpan, final int alignment,
			final boolean grabHorizontalSpace, final boolean grabVerticalSpace) {
		final GridData result = new GridData();
		result.grabExcessHorizontalSpace = grabHorizontalSpace;
		result.grabExcessVerticalSpace = grabVerticalSpace;
		result.horizontalAlignment = alignment;
		result.verticalAlignment = alignment;
		result.horizontalSpan = horizontalSpan;
		result.verticalSpan = verticalSpan;
		return result;
	}

	public static GridData constructGridData(final int horizontalSpan,
			final int verticalSpan, final int alignment) {
		return Main.constructGridData(horizontalSpan, verticalSpan, alignment,
				false);
	}

	public static GridData constructGridData(final int horizontalSpan,
			final int verticalSpan) {
		return Main.constructGridData(horizontalSpan, verticalSpan, SWT.FILL,
				false);
	}

	static Shell form;
	static Text input, output;

	public static void main(final String[] args) {
		Main.form = new Shell(Main.getDisplay());
		Main.input = new Text(Main.form, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		Main.output = new Text(Main.form, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);

		final Button getLinks = new Button(Main.form, SWT.PUSH | SWT.CENTER);

		getLinks.setText("Links auflösen");
		getLinks.addSelectionListener(Main.getLinksListener);

		Main.form.setLayout(new RowLayout(SWT.VERTICAL));
		Main.input.setLayoutData(new RowData(800, 250));
		Main.output.setLayoutData(new RowData(800, 250));

		Main.form.pack();
		Main.form.open();
		while (!Main.form.isDisposed()) {
			if (!Main.getDisplay().readAndDispatch()) {
				Main.getDisplay().sleep();
			}
		}
		Main.form.dispose();
	}

	static String getForwardUrl(final String url) throws IOException {

		final URLConnection conn = new URL(url).openConnection();
		conn.getInputStream();
		return conn.getURL().toString();
	}

	static String getURLContent(final String url) throws MalformedURLException,
			IOException {
		return Main.getURLContent(url, null);
	}

	static String getURLContent(final String url, final String postData)
			throws MalformedURLException, IOException {

		final URLConnection conn = new URL(url).openConnection();
		if (postData != null) {
			conn.setDoOutput(true);
			final OutputStream os = conn.getOutputStream();
			os.write(postData.getBytes());
			os.flush();
			os.close();
		}

		final BufferedInputStream bis = new BufferedInputStream(
				conn.getInputStream());
		final StringBuilder content = new StringBuilder();
		int ding;
		while ((ding = bis.read()) != -1) {
			if ((ding != 13) && (ding != 10)) {
				content.append((char) ding);
			}
			if ((content.length() == 3)
					&& content.toString().equalsIgnoreCase("rar")) {
				// Falls wir ausversehen eine Datei runterladen
				content.append("location=\"");
				content.append(url);
				content.append("\"");
				break;
			}
		}
		bis.close();
		return content.toString();
	}

	static SelectionListener getLinksListener = new SelectionListener() {

		@Override
		public void widgetSelected(final SelectionEvent e) {
			((Button) e.widget).setEnabled(false);
			final StringTokenizer input = new StringTokenizer(
					Main.input.getText());

			final Pattern framePattern = Pattern
					.compile(".*\\<FRAME\\s*SRC=\"([^\"]*)\"");
			final Pattern formPattern = Pattern
					.compile(".*\\<FORM[^\\>]*\\>(.*)\\<\\/FORM\\>.*");
			final Pattern hiddenInputPattern = Pattern
					.compile(".*\\<INPUT[^\\>]*NAME=\"s\"[^\\>]*VALUE=\"([^\"]*)\".*\\>.*");
			final Pattern captchaImagePattern = Pattern
					.compile(".*\\<IMG.*SRC=\"([^\"]*)\".*");
			final Pattern downloadLinkPattern = Pattern
					.compile("(http:\\/\\/download\\.serienjunkies\\.org\\/go-[^\"]*)\"");
			final Pattern frameDownloadLinkPattern = Pattern
					.compile("location=\\\"(\\/#\\!download\\|\\d+\\|\\d+\\|[^\\|]+\\|\\d+)");

			Main.output.setText("");
			outer: while (input.hasMoreElements()) {
				String link = input.nextToken();
				String baseURL = link.replaceAll("(http://[^/]*)/.*", "$1");
				final boolean isOld = link.contains("safe");

				int linkCount = 0;
				String content = null;
				do {
					try {
						if (content == null) {
							content = Main.getURLContent(link);
						}

						if (content.contains("FRAMESET")) {
							final Matcher frame = framePattern.matcher(content);
							frame.find();
							content = Main.getURLContent(frame.group(1));
							link = frame.group(1);
							baseURL = link
									.replaceAll("(http://[^/]*)/.*", "$1");
						}

						final Matcher m = formPattern.matcher(content);
						if (m.find()) {
							final String formContent = m.group(1);
							final Matcher hiddenInputMatcher = hiddenInputPattern
									.matcher(formContent);
							final Matcher captchaImageMatcher = captchaImagePattern
									.matcher(formContent);

							hiddenInputMatcher.find();
							captchaImageMatcher.find();

							final String checksum = hiddenInputMatcher.group(1);
							final String captchaSource = captchaImageMatcher
									.group(1);

							if (captchaSource.equals("/help/nocaptcha/nc.gif")) {
								final MessageBox d = new MessageBox(Main.form);
								d.setText("Downloadlimit erreicht");
								d.setMessage("Das Downloadlimit wurde erreicht. Wechseln Sie Ihre IP oder versuchen Sie es später noch einmal.");
								d.open();
								break outer;
							}

							final Image captchaImage = new Image(
									Main.getDisplay(), new URL(baseURL
											+ captchaSource).openConnection()
											.getInputStream());

							final String captcha = new CaptchaInput()
									.getCaptcha(captchaImage);

							if (captcha != null) {
								// Direkt wieder in content - falls das Captcha
								// falsch war gibts schon ein neues
								final String content2 = Main
										.getURLContent(link, "s=" + checksum
												+ "&c=" + captcha);

								content = content2;
								Matcher downloadLinkMatcher;
								if (isOld) {
									downloadLinkMatcher = frameDownloadLinkPattern
											.matcher(content);
								} else {
									downloadLinkMatcher = downloadLinkPattern
											.matcher(content);
								}
								while (downloadLinkMatcher.find()) {
									Main.output
											.append((isOld ? "http://rapidshare.com"
													+ downloadLinkMatcher
															.group(1)
													: Main.getForwardUrl(downloadLinkMatcher
															.group(1)))
													+ "\n");
									linkCount++;
									content = null;
								}
							} else {
								break outer;
							}
						}

					} catch (final Exception ex) {
						linkCount = -1;
					}
				} while (linkCount == 0);
			}
			((Button) e.widget).setEnabled(true);
			java.awt.Toolkit.getDefaultToolkit().beep();
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent e) {
		}
	};
}
