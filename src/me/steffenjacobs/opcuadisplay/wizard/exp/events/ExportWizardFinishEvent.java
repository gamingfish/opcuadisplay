package me.steffenjacobs.opcuadisplay.wizard.exp.events;

import me.steffenjacobs.opcuadisplay.shared.util.EventBus.Event;
import me.steffenjacobs.opcuadisplay.shared.util.EventBus.EventArgs;

public class ExportWizardFinishEvent extends Event {

	public static String IDENTIFIER = "exportWizardFinish";

	private final String url;

	public ExportWizardFinishEvent(String url) {
		super(IDENTIFIER, EventArgs.NONE);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
