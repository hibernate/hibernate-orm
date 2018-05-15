/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.walking;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.MetamodelGraphWalker;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.loadplans.process.EncapsulatedCompositeAttributeResultSetProcessorTest.Person;
import org.hibernate.test.loadplans.process.EncapsulatedCompositeAttributeResultSetProcessorTest.Customer;

/**
 * @author Steve Ebersole
 */
public class NestedCompositeElementTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, Customer.class };
	}

	@Test
	public void testWalkingKeyManyToOneGraphs() {
		final EntityPersister ep = (EntityPersister) sessionFactory().getClassMetadata( Customer.class );
		MetamodelGraphWalker.visitEntity( new LoggingAssociationVisitationStrategy(), ep );
	}
}
