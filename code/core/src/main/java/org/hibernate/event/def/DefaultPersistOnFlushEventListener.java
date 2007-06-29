//$Id: DefaultPersistOnFlushEventListener.java 9019 2006-01-11 18:50:33Z epbernard $
package org.hibernate.event.def;

import org.hibernate.engine.CascadingAction;

/**
 * When persist is used as the cascade action, persistOnFlush should be used
 * @author Emmanuel Bernard
 */
public class DefaultPersistOnFlushEventListener extends DefaultPersistEventListener {
	protected CascadingAction getCascadeAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}
}
