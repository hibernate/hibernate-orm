/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.removal;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwnedEntity;
import org.hibernate.orm.test.envers.entities.manytomany.ListOwningEntity;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Jpa(annotatedClasses = {
		StrTestEntity.class, ListOwnedEntity.class, ListOwningEntity.class
}, integrationSettings = @Setting(name = "org.hibernate.envers.cascade_delete_revision", value = "true"))
public class RemoveDefaultRevisionEntity extends AbstractRevisionEntityRemovalTest {
	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdRevisionEntity.class;
	}
}
