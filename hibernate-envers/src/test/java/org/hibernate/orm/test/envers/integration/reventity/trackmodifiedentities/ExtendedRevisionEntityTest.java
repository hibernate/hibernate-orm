/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.ExtendedRevisionEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.ExtendedRevisionListener;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests proper behavior of revision entity that extends {@link DefaultTrackingModifiedEntitiesRevisionEntity}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class, StrIntTestEntity.class, ExtendedRevisionEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, value = "false"))
@SessionFactory
public class ExtendedRevisionEntityTest extends DefaultTrackingEntitiesTest {
	@Test
	public void testCommentPropertyValue(SessionFactoryScope scope) {
		scope.inSession( session -> {
			ExtendedRevisionEntity ere = AuditReaderFactory.get( session )
					.findRevision( ExtendedRevisionEntity.class, 1 );

			assertEquals( ExtendedRevisionListener.COMMENT_VALUE, ere.getComment() );
		} );
	}
}
