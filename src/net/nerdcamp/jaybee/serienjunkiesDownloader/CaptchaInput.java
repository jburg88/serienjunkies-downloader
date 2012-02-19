package net.nerdcamp.jaybee.serienjunkiesDownloader;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CaptchaInput {
	public static String captchaString = null;

	public String getCaptcha(final Image captchaImage) {
		final Shell form = new Shell(Main.getDisplay());
		final Label captcha = new Label(form, SWT.CENTER);
		final Text captchaInput = new Text(form, SWT.BORDER);
		final Button submitCaptcha = new Button(form, SWT.PUSH | SWT.CENTER);

		form.setText("Captcha input");
		captcha.setImage(captchaImage);
		submitCaptcha.setText("OK");
		form.setDefaultButton(submitCaptcha);
		submitCaptcha.addSelectionListener(new SelectionListener() {
			public void widgetSelected(final SelectionEvent e) {
				CaptchaInput.captchaString = captchaInput.getText();
				form.dispose();
			}

			public void widgetDefaultSelected(final SelectionEvent e) {
			}
		});

		form.setLayout(new RowLayout(SWT.VERTICAL));
		captcha.setLayoutData(new RowData(120, 60));
		captchaInput.setLayoutData(new RowData(100, 18));
		submitCaptcha.setLayoutData(new RowData(100, 18));

		form.pack();
		form.open();
		while (!form.isDisposed()) {
			if (!Main.getDisplay().readAndDispatch()) {
				Main.getDisplay().sleep();
			}
		}
		form.dispose();
		return CaptchaInput.captchaString;
	}
}
