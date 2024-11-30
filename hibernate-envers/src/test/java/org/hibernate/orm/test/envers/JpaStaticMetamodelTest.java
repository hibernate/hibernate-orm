/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import org.hibernate.envers.DefaultRevisionEntity_;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity_;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity_;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity_;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Just a test to make sure that the generated "JPA static metamodel" classes continue to be generated.
 *
 * @author Steve Ebersole
 */
public class JpaStaticMetamodelTest extends BaseUnitTestCase {
	@Test
	public void testStaticMetamodelGenerationHappened() {
		// Really in a regression scenario, this class wont even compile...
		assertNotNull( DefaultRevisionEntity_.class );
		assertNotNull( DefaultTrackingModifiedEntitiesRevisionEntity_.class );
		assertNotNull( SequenceIdRevisionEntity_.class );
		assertNotNull( SequenceIdTrackingModifiedEntitiesRevisionEntity_.class );
	}
}
