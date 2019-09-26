package org.sireum.aadl.osate.hamr;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		// setDescription("Code generation configuration page");
	}

	@Override
	protected void createFieldEditors() {
		// TabFolder tabFolder = new TabFolder(getFieldEditorParent(), SWT.NONE);
		// tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Composite comp = addTab(tabFolder, "Code Generation");
		Composite comp = getFieldEditorParent();

		addField(new BooleanFieldEditor(PreferenceValues.HAMR_DEVICES_AS_THREADS_OPT, "Treat AADL devices as threads",
				comp));

		addField(new BooleanFieldEditor(PreferenceValues.HAMR_EMBED_ART_OPT, "Embed ART", comp));

		addField(new BooleanFieldEditor(PreferenceValues.HAMR_SERIALIZE_OPT,
				"Serialize AIR to JSON (non-compact) when generating CAmkES", comp));

		addField(new BooleanFieldEditor(PreferenceValues.HAMR_VERBOSE_OPT, "Verbose output", comp));

		addField(new StringFieldEditor(PreferenceValues.HAMR_OUTPUT_FOLDER_OPT, "Output folder", comp));

		addField(new StringFieldEditor(PreferenceValues.HAMR_SIREUM_HOME, "SIREUM_HOME", comp));
	}

	private Composite addTab(TabFolder tabFolder, String tabName) {
		Composite newTab = new Composite(tabFolder, SWT.NULL);
		newTab.setLayout(new GridLayout());
		newTab.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		TabItem item = new TabItem(tabFolder, SWT.NONE);
		item.setText(tabName);
		item.setControl(newTab);

		return newTab;
	}

	@Override
	public void init(IWorkbench workbench) {

	}
}
