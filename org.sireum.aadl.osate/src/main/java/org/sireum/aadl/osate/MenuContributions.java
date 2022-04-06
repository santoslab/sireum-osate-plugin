package org.sireum.aadl.osate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.sireum.aadl.osate.PreferenceValues.Generators;

public class MenuContributions extends CompoundContributionItem {

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> l = new ArrayList<IContributionItem>();

		l.add(getItem("Serialize AIR to file", "org.sireum.commands.launchsireum",
				map("org.sireum.commands.launchsireum.generator", Generators.SERIALIZE.toString())));

		l.add(new org.eclipse.jface.action.Separator());

		return l.toArray(new IContributionItem[0]);
	}

	private IContributionItem getItem(String label, String commandId, Map<String, String> params) {

		return new CommandContributionItem(
				new CommandContributionItemParameter(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), // serviceLocator - non-null
						null, // id
						commandId, // commandId
						params, // parameters
						null, // icon
						null, // disabledIcon
						null, // hoverIcon
						label, // label
						null, // mnemonic
						null, // tooltip
						CommandContributionItem.STYLE_PUSH, // style,
						null, // helpContextId,
						false // visibleEnabled
				)) {

		};
	}

	private Map<String, String> map(String... args) {
		assert (args.length % 2 == 0);
		Map<String, String> m = new HashMap<String, String>();
		for (int i = 0; i < args.length; i += 2) {
			m.put(args[i], args[i + 1]);
		}
		return m;
	}
}
