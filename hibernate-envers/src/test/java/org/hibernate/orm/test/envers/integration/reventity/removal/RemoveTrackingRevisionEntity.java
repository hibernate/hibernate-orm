/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.removal;

import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
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
}, integrationSettings = {
		@Setting(name = "org.hibernate.envers.cascade_delete_revision", value = "true"),
		@Setting(name = "org.hibernate.envers.track_entities_changed_in_revision", value = "true")
})
public class RemoveTrackingRevisionEntity extends AbstractRevisionEntityRemovalTest {

	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
	}
}
