package org.hibernate.envers.test.integration.reventity.removal;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RemoveDefaultRevisionEntity extends AbstractRevisionEntityRemovalTest {
	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdRevisionEntity.class;
	}
}
