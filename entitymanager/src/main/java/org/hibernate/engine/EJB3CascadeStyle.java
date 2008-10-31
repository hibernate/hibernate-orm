//$Id: $
package org.hibernate.engine;

/**
 * Becasue CascadeStyle is not opened and package protected,
 * I need to subclass and override the persist alias
 *
 * Note that This class has to be triggered by EJB3PersistEventListener at class loading time
 *
 * TODO get rid of it for 3.3
 *
 * @author Emmanuel Bernard
 */
public abstract class EJB3CascadeStyle extends CascadeStyle {

	/**
	 * cascade using EJB3CascadingAction
	 */
	public static final CascadeStyle PERSIST_EJB3 = new CascadeStyle() {
		public boolean doCascade(CascadingAction action) {
			return action==EJB3CascadingAction.PERSIST_SKIPLAZY
					|| action==CascadingAction.PERSIST_ON_FLUSH;
		}
		public String toString() {
			return "STYLE_PERSIST_SKIPLAZY";
		}
	};

	static {
		STYLES.put( "persist", PERSIST_EJB3 );
	}
}
