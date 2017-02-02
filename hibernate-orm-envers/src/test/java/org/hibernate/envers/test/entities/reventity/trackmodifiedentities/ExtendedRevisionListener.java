/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import org.hibernate.envers.RevisionListener;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ExtendedRevisionListener implements RevisionListener {
	public static final String COMMENT_VALUE = "Comment";

	public void newRevision(Object revisionEntity) {
		((ExtendedRevisionEntity) revisionEntity).setComment( COMMENT_VALUE );
	}
}
