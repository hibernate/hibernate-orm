/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.basic;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.SessionImplementor;

public class PersistentMyList extends PersistentList implements IMyList {

	public PersistentMyList(SessionImplementor session) {
		super(session);
	}

	public PersistentMyList(SessionImplementor session, IMyList list) {
		super(session, list);
	}

	
	
}
