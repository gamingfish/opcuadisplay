package me.steffenjacobs.opcuadisplay.ui.views.explorer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import me.steffenjacobs.opcuadisplay.eventbus.EventBus;
import me.steffenjacobs.opcuadisplay.management.node.NodeNavigator;
import me.steffenjacobs.opcuadisplay.management.node.domain.CachedBaseNode;
import me.steffenjacobs.opcuadisplay.opcInterface.opcClient.OPCUaClient;
import me.steffenjacobs.opcuadisplay.ui.views.explorer.events.RootUpdatedEvent;
/** @author Steffen Jacobs */
public class OpcUaConnector implements ITreeContentProvider {

	private final Shell parentShell;

	public OpcUaConnector(Shell parentShell) {
		this.parentShell = parentShell;
	}

	/**
	 * loads the variables found on the URL <i>url</i> and makes the loaded tree
	 * available at NodeNavigator.root
	 */
	public void loadVariables(final String url) {
		Job job = new Job("Downloading OPC UA nodes...") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					NodeNavigator.getInstance().setRoot(new OPCUaClient().retrieveNodes(url, monitor));

					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							EventBus.getInstance()
									.fireEvent(new RootUpdatedEvent(NodeNavigator.getInstance().getRoot()));
						}
					});
					return Status.OK_STATUS;
				} catch (Exception e) {
					e.printStackTrace();
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(parentShell, "OPC UA Display", e.getLocalizedMessage());
						}
					});
					return Status.CANCEL_STATUS;
				}
			}
		};

		job.setUser(true);
		job.schedule();
	}

	@Override
	public Object[] getChildren(Object arg0) {
		if (arg0 instanceof CachedBaseNode) {
			return ((CachedBaseNode) arg0).getChildren();
		}
		return null;
	}

	@Override
	public Object[] getElements(Object arg0) {
		return new CachedBaseNode[]
			{ NodeNavigator.getInstance().getRoot() };
	}

	@Override
	public Object getParent(Object arg0) {
		if (arg0 instanceof CachedBaseNode) {
			return ((CachedBaseNode) arg0).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object arg0) {
		if (arg0 instanceof CachedBaseNode) {
			return ((CachedBaseNode) arg0).hasChildren();
		}
		return false;
	}

}
