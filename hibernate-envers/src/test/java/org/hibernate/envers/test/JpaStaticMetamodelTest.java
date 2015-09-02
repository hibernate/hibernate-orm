/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

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
