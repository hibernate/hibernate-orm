/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.core;

import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;

/**
 * @author Emmanuel Bernard
 */
public class JpaPersistOnFlushEventListener extends JpaPersistEventListener {
	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingActions.PERSIST_ON_FLUSH;
	}
}
