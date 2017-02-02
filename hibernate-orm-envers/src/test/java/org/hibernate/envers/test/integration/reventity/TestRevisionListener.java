/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import org.hibernate.envers.RevisionListener;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestRevisionListener implements RevisionListener {
	public static String data = "data0";

	public void newRevision(Object revisionEntity) {
		((ListenerRevEntity) revisionEntity).setData( data );
	}
}
