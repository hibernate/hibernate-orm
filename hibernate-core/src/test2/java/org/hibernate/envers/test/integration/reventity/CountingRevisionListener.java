/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import org.hibernate.envers.RevisionListener;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CountingRevisionListener implements RevisionListener {
	public static int revisionCount = 0;

	@Override
	public void newRevision(Object revisionEntity) {
		++revisionCount;
	}
}
