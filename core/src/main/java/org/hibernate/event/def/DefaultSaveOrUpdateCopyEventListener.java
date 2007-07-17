//$Id: DefaultSaveOrUpdateCopyEventListener.java 7793 2005-08-10 05:06:40Z oneovthafew $
package org.hibernate.event.def;

import org.hibernate.engine.CascadingAction;

public class DefaultSaveOrUpdateCopyEventListener extends DefaultMergeEventListener {

	protected CascadingAction getCascadeAction() {
		return CascadingAction.SAVE_UPDATE_COPY;
	}

}
