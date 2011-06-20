package wrims.schematic;

import java.awt.Container;
import javax.swing.Icon;

//
// depending on the current context.
/**
 * Define an Action that knows about views and supports enabling/disabling depending on the
 * current context.
 * @author Tom Pruett
 * @author Clay Booher
 */
public abstract class SchematicRelatedAction extends ContainerRelatedAction {
//	public Schematic getApp() {
//		return (Schematic) myApp;
//	}

	public SchematicView getView() {
		if (getApp() != null && getApp() instanceof Schematic) {
			return ((Schematic)getApp()).getCurrentView();
		} else return null;
	}

	public Plotter getPlotter() {
		if (getApp() != null && getApp() instanceof Schematic) {
			return ((Schematic)getApp()).getPlotter();
		} else return null;
	}

	public SchematicRelatedAction(String name, Container app) {
		super(name, app);
	}

	public SchematicRelatedAction(String name, Icon icon, Container app) {
		super(name, icon, app);
	}

	// by default each AppAction is disabled if there's no current view
	public boolean canAct() {
//		System.out.println(toString() + "'s canAct() fired");
		return (getView() != null);
	}
}
