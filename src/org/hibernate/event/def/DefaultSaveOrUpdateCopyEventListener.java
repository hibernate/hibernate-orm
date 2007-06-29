//$Id$
package org.hibernate.event.def;

import org.hibernate.engine.CascadingAction;

public class DefaultSaveOrUpdateCopyEventListener extends DefaultMergeEventListener {

	protected CascadingAction getCascadeAction() {
		return CascadingAction.SAVE_UPDATE_COPY;
	}

}
