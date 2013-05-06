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
