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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Main {

	final static Pattern FRAME_PATTERN = Pattern
			.compile(".*\\<FRAME\\s*SRC=\"([^\"]*)\"");
	final static Pattern FORM_PATTERN = Pattern
			.compile(".*\\<FORM[^\\>]*\\>(.*)\\<\\/FORM\\>.*");
	final static Pattern HIDDEN_INPUT_PATTERN = Pattern
			.compile(".*\\<INPUT[^\\>]*NAME=\"s\"[^\\>]*VALUE=\"([^\"]*)\".*\\>.*");
	final static Pattern CAPTCHA_IMAGE_PATTERN = Pattern
			.compile(".*\\<IMG.*SRC=\"([^\"]*)\".*");
	final static Pattern DOWNLOADLINK_PATTERN = Pattern
			.compile("(http:\\/\\/download\\.serienjunkies\\.org\\/go-[^\"]*)\"");
	final static Pattern FRAME_DOWNLOADLINK_PATTERN = Pattern
			.compile("location=\\\"(\\/#\\!download\\|\\d+\\|\\d+\\|[^\\|]+\\|\\d+)");

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
		final Label inputLabel = new Label(Main.form, 0);
		Main.input = new Text(Main.form, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		final Button resolveLinks = new Button(Main.form, SWT.PUSH | SWT.CENTER);
		final Label outputLabel = new Label(Main.form, 0);
		Main.output = new Text(Main.form, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);

		Main.form.setText("Serienjunkies.org link resolver");
		inputLabel.setText("Paste links to resolve and click 'resolve links':");
		outputLabel.setText("Resolved links:");
		resolveLinks.setText("resolve links");
		resolveLinks.addSelectionListener(Main.resolveLinksListener);

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
				// to prevent downloading of files
				content.append("location=\"");
				content.append(url);
				content.append("\"");
				break;
			}
		}
		bis.close();
		return content.toString();
	}

	static SelectionListener resolveLinksListener = new SelectionListener() {

		public void widgetSelected(final SelectionEvent e) {
			((Button) e.widget).setEnabled(false);
			Main.input.setEnabled(false);
			final StringTokenizer input = new StringTokenizer(
					Main.input.getText(), "\r\n");

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
							final Matcher frame = Main.FRAME_PATTERN
									.matcher(content);
							frame.find();
							content = Main.getURLContent(frame.group(1));
							link = frame.group(1);
							baseURL = link
									.replaceAll("(http://[^/]*)/.*", "$1");
						}

						final Matcher m = Main.FORM_PATTERN.matcher(content);
						if (m.find()) {
							final String formContent = m.group(1);
							final Matcher hiddenInputMatcher = Main.HIDDEN_INPUT_PATTERN
									.matcher(formContent);
							final Matcher captchaImageMatcher = Main.CAPTCHA_IMAGE_PATTERN
									.matcher(formContent);

							hiddenInputMatcher.find();
							captchaImageMatcher.find();

							final String checksum = hiddenInputMatcher.group(1);
							final String captchaSource = captchaImageMatcher
									.group(1);

							if (captchaSource.equals("/help/nocaptcha/nc.gif")) {
								final MessageBox d = new MessageBox(Main.form);
								d.setText("Downloadlimit reached");
								d.setMessage("Downloadlimit was reached. Change your IP or try again later.");
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
								// save result to content. If captcha is wrong,
								// the new captcha is already delivered
								final String content2 = Main
										.getURLContent(link, "s=" + checksum
												+ "&c=" + captcha);

								content = content2;
								Matcher downloadLinkMatcher;
								if (isOld) {
									downloadLinkMatcher = Main.FRAME_DOWNLOADLINK_PATTERN
											.matcher(content);
								} else {
									downloadLinkMatcher = Main.DOWNLOADLINK_PATTERN
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
			Main.input.setEnabled(false);
		}

		public void widgetDefaultSelected(final SelectionEvent e) {
		}
	};
}
