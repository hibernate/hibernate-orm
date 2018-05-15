/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;

/**
 * @author Emmanuel Bernard
 */
public class NoOpListener implements PreInsertEventListener {
	public boolean onPreInsert(PreInsertEvent event) {
		return false;
	}
}
