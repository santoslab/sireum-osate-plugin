package org.sireum.aadl.osate.awas.handlers;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osate.aadl2.Element;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.modelsupport.EObjectURIWrapper;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.ge.CanonicalBusinessObjectReference;
import org.osate.ge.GraphicalEditor;
import org.osate.ge.gef.AgeGefRuntimeException;
import org.osate.ge.gef.ui.editor.AgeEditor;
import org.osate.ge.graphics.Style;
import org.osate.ge.graphics.StyleBuilder;
import org.osate.ge.internal.diagram.runtime.DiagramElement;
import org.osate.ge.internal.services.ActionExecutor.ExecutionMode;
import org.osate.ge.internal.services.DiagramService;
import org.osate.ge.internal.services.DiagramService.DiagramReference;
import org.osate.ge.internal.ui.util.EditorUtil;
import org.osate.ui.UiUtil;
import org.sireum.aadl.osate.awas.Activator;
import org.sireum.aadl.osate.awas.util.AwasServer;
import org.sireum.aadl.osate.awas.util.AwasUtil;
import org.sireum.aadl.osate.handlers.AbstractSireumHandler;
import org.sireum.aadl.osate.util.Util;
import org.sireum.hamr.ir.Aadl;
import org.sireum.message.Reporter;

public class AwasServerHandler extends AbstractSireumHandler implements IElementUpdater {

	boolean isServerOn = false;

	UIElement buttonIcon = null;

	AwasServer server = null;
	static Display display = null;

	static DiagramService diagramService = null;
	static AgeEditor ade = null;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ComponentInstance root = getComponentInstance(event);
		Command command = event.getCommand();
		display = shell.getDisplay();
		boolean oldValue = HandlerUtil.toggleCommandState(command);
		diagramService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(DiagramService.class);
		if (!oldValue && root == null) {
			MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
			return null;
		}
		MessageConsole console = displayConsole("Awas Console");

		ImageDescriptor icon = null;

		// change listener

		IResourceChangeListener listener = new IResourceChangeListener() {
			public void printDelta(IResourceDelta d) {

				IResource resource = d.getResource();


				for (IResourceDelta childDelta : d.getAffectedChildren()) {
					IResource res2 = childDelta.getResource();

					printDelta(childDelta);
					if (res2.getType() == IResource.FILE) {
						Resource res = root.eResource();
						String s1 = ((File) res2).getFullPath().toString();
						String s2 = Util.toIFile(res.getURI()).getFullPath().toPortableString();
						if (s1.equals(s2)) {
							Element elem = AadlUtil.getElement(res2);
							if (elem instanceof ComponentInstance) {
								ComponentInstance root2 = (ComponentInstance) elem;
								Reporter reporter = Util.createReporter();
								Aadl air2 = Util.getAir(root2, true, console, reporter);
								if(reporter.hasError()) {
									// TODO probably should handle this. The errors could be
									// converted to markers -- see harm plugin
								}
								org.sireum.awas.ast.Model awasModel2 = org.sireum.awas.AADLBridge.AadlHandler
										.buildAwasModel(air2);

								server.updateModel(awasModel2);

							}
							break;
						}
					}
				}
			}

			@Override
			public void resourceChanged(IResourceChangeEvent e) {

				if (e.getType() == IResourceChangeEvent.POST_CHANGE) {

					IResourceDelta delta = e.getDelta();
					printDelta(delta);

				}
			}
		};



		// end of change listener

		if (!oldValue) {
			final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);



			if (root == null && !isServerOn) {
				MessageDialog.openError(shell, "Sireum", "Please select a system implementation or a system instance");
				return null;
			}

			IProject currProject = getProject(root);// SelectionHelper.getProject();
			Set<IProject> projects = new HashSet();
			projects.add(currProject);
			diagramService.findDiagrams(projects).forEach(dr -> {
				if (dr.isValid()) {
					Resource res = root.eResource();
					URI uri = res.getURI();
					IPath instancePath = Util.toIFile(uri).getFullPath();

					CanonicalBusinessObjectReference bor = dr.getContextReference();
					if (bor.getSegments().stream().anyMatch(it -> it.equals(instancePath.toString().toLowerCase()))) {
						// ade = diagramService.openOrCreateDiagramForBusinessObject(dr);
						ade = getAgeDiagramEditor(dr);
					}
				}
			});
			// des.addAll(AwasUtil.getAllDiagramElements(ade.getDiagramBehavior().getAgeDiagram()));

//			Set<IProject> projects = new HashSet();
//			projects.add(currProject);

			Reporter reporter = Util.createReporter();
			Aadl air = Util.getAir(root, true, console, reporter);

			if (air != null && !reporter.hasError()) {

				org.sireum.awas.ast.Model awasModel = org.sireum.awas.AADLBridge.AadlHandler.buildAwasModel(air);
				server = new AwasServer(awasModel, root.getSystemInstance(),
						HandlerUtil.getActiveWorkbenchWindowChecked(event).getActivePage());
			} else {
				// TODO could convert errors to markes -- see hamr plugin
			}
		}

		if (!oldValue && server != null) {
			isServerOn = true;
			icon = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.getDefault().getBundle().getSymbolicName(),
					"icons/stop.png");
			ResourcesPlugin.getWorkspace().addResourceChangeListener(listener);
			server.startServer();

		} else {
			isServerOn = false;
			icon = AbstractUIPlugin.imageDescriptorFromPlugin(Activator.getDefault().getBundle().getSymbolicName(),
					"icons/play.png");
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
			if (server != null) {

			server.shutdownServer();
		}
		}
		buttonIcon.setIcon(icon);

		return null;
	}

	@SuppressWarnings("restriction")
	private static AgeEditor getAgeDiagramEditor(DiagramReference diagramRef) {

		GraphicalEditor ge = EditorUtil.openEditor(diagramRef.getFile(), false);// diagramService.openOrCreateDiagramForBusinessObject(diagramRef.getContextReference());

		if (!(ge instanceof AgeEditor)) {
			throw new AgeGefRuntimeException("Unexpected editor type. Editor must be of type " + AgeEditor.class);
		}

		return (AgeEditor) ge;
//		return null;

//		if (diagramRef.isOpen()) {
//			return diagramRef.getEditor();
//		} else {
//			return EditorUtil.openEditor(diagramRef.getFile(), false);
//		}

	}

	@Override
	protected IStatus runJob(Element arg0, IProgressMonitor arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		// TODO Auto-generated method stub
		// AbstractUIPlugin.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/stop.png");
		this.buttonIcon = element;

		// ImageDescriptor.createFromFile(this.getClass(), "icons/stop.png");
		ImageDescriptor icon = AbstractUIPlugin
				.imageDescriptorFromPlugin(Activator.getDefault().getBundle().getSymbolicName(), "icons/play.png");
		element.setIcon(icon);
	}

	@SuppressWarnings("restriction")
	public static void highlightInstanceDiagram(Map<URI, String> uris, SystemInstance root) {

		display.syncExec(() -> {
			Set<DiagramElement> des = new HashSet<DiagramElement>();


			AwasUtil.getAllDiagramElements(ade.getDiagram()).forEach(de -> des.add(de));

			final EObjectURIWrapper.Factory factory = new EObjectURIWrapper.Factory(
					UiUtil.getModelElementLabelProvider());

			des.forEach(de -> {
				URI hUri = factory.createWrapperFor((EObject) de.getBusinessObject()).getUri();
				if (de.getBusinessObject() instanceof EObject && uris.containsKey(hUri)) {
					de.setStyle(StyleBuilder.create(de.getStyle()).backgroundColor(AwasUtil.hex2Rgb(uris.get(hUri)))
							// .fontColor(org.osate.ge.graphics.Color.ORANGE)
							.outlineColor(AwasUtil.hex2Rgb(uris.get(hUri))).build());
				}

			});


			ade.getActionExecutor().execute("highlight diagram", ExecutionMode.NORMAL, () -> {
				ade.updateNowIfModelHasChanged();
				ade.updateDiagram();
				ade.getGefDiagram().refreshDiagramStyles();
				ade.doSave(new NullProgressMonitor());
				return null;
			});
//
//			ade.forceDiagramUpdateOnNextModelChange();
//			ade.updateDiagram();
//			ade.setFocus();
//			ade.clearSelection();

			// ade.updateNowIfModelHasChanged();
			// ade.doSave(new NullProgressMonitor());
		});


	}

	@SuppressWarnings("restriction")
	public static void clearInstanceDiagram(Set<URI> iUri, SystemInstance root) {
		display.syncExec(() -> {
			Set<DiagramElement> des = new HashSet<DiagramElement>();

			AwasUtil.getAllDiagramElements(ade.getDiagram()).forEach(de -> des.add(de));

			final EObjectURIWrapper.Factory factory = new EObjectURIWrapper.Factory(
					UiUtil.getModelElementLabelProvider());

			des.forEach(de -> {
				URI hUri = factory.createWrapperFor((EObject) de.getBusinessObject()).getUri();
				if (de.getBusinessObject() instanceof EObject && iUri.contains(hUri)) {
					de.setStyle(Style.DEFAULT);
				}
			});

			ade.getActionExecutor().execute("highlight diagram", ExecutionMode.NORMAL, () -> {
				ade.updateNowIfModelHasChanged();
				ade.updateDiagram();
				ade.getGefDiagram().refreshDiagramStyles();
				ade.doSave(new NullProgressMonitor());
				return null;
			});
		});
	}


}
