package me.steffenjacobs.opcuadisplay.ui.wizard.exp.events;

import me.steffenjacobs.opcuadisplay.management.event.eventbus.EventBus.Event;
import me.steffenjacobs.opcuadisplay.management.event.eventbus.EventBus.EventArgs;
/** @author Steffen Jacobs */
public class ExportWizardOpenEvent extends Event {

	public static String IDENTIFIER = "exportWizardOpen";

	public ExportWizardOpenEvent() {
		super(IDENTIFIER, EventArgs.NONE);
	}
}
