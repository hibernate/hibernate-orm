/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.reventity.trackmodifiedentities.AnnotatedTrackingRevisionEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests proper behavior of revision entity that utilizes {@link ModifiedEntityNames} annotation.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class, StrIntTestEntity.class, AnnotatedTrackingRevisionEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, value = "false"))
@SessionFactory
public class AnnotatedTrackingEntitiesTest extends DefaultTrackingEntitiesTest {
}
