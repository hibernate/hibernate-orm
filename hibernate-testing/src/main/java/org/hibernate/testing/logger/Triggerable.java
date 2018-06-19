/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import java.util.Collections;
import java.util.List;

public interface Triggerable {

	String triggerMessage();

	boolean wasTriggered();

	void reset();

	default List<String> triggerMessages() {
		return Collections.singletonList( triggerMessage() );
	}
}
