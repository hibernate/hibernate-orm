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
