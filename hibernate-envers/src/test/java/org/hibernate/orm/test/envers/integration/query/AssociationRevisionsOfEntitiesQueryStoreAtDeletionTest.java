/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-13817")
@Jpa(
		annotatedClasses = {
				AssociationRevisionsOfEntitiesQueryTest.Template.class,
				AssociationRevisionsOfEntitiesQueryTest.TemplateType.class
		},
		integrationSettings = {
				@Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true")
		}
)
@EnversTest
public class AssociationRevisionsOfEntitiesQueryStoreAtDeletionTest extends AssociationRevisionsOfEntitiesQueryTest {
	@Override
	protected boolean isStoreDataAtDelete(EntityManagerFactoryScope scope) {
		return true;
	}
}
