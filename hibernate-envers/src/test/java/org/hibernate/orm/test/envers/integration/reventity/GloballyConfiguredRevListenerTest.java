/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-6696")
@EnversTest
@Jpa(annotatedClasses = {StrTestEntity.class},
		integrationSettings = @Setting(name = EnversSettings.REVISION_LISTENER, value = "org.hibernate.orm.test.envers.integration.reventity.CountingRevisionListener"))
public class GloballyConfiguredRevListenerTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		CountingRevisionListener.revisionCount = 0;

		// Revision 1
		scope.inTransaction( em -> {
			StrTestEntity te = new StrTestEntity( "data" );
			em.persist( te );
		} );

		assertEquals( 1, CountingRevisionListener.revisionCount );
	}
}
