/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.persister;

import org.junit.Test;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Shawn Clowater
 */
public class PersisterTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testEntityEntityPersisterAndPersisterSpecified() throws Exception {
		//checks to see that the persister specified with the @Persister annotation takes precedence if a @Entity.persister() is also specified		
		if ( isMetadataUsed() ) {
			Class<? extends org.hibernate.persister.entity.EntityPersister> clazz =
					metadata().getEntityBinding( Deck.class.getName() ).getCustomEntityPersisterClass();
			assertEquals( "Incorrect Persister class for " + Deck.class.getName(),
					org.hibernate.test.annotations.persister.EntityPersister.class, clazz );

		}
		else {
			PersistentClass persistentClass = configuration().getClassMapping( Deck.class.getName() );
			assertEquals( "Incorrect Persister class for " + persistentClass.getMappedClass(), EntityPersister.class,
					persistentClass.getEntityPersisterClass() );
		}
	}

	@Test
	public void testEntityEntityPersisterSpecified() throws Exception {
		//tests the persister specified with an @Entity.persister()		
		if ( isMetadataUsed() ) {
			Class<? extends  org.hibernate.persister.entity.EntityPersister> clazz =
					metadata().getEntityBinding( Card.class.getName() ).getCustomEntityPersisterClass();
			assertEquals( "Incorrect Persister class for " + Card.class.getName(),
					SingleTableEntityPersister.class, clazz );

		}
		else {
			PersistentClass persistentClass = configuration().getClassMapping( Card.class.getName() );
			assertEquals( "Incorrect Persister class for " + persistentClass.getMappedClass(),
					SingleTableEntityPersister.class, persistentClass.getEntityPersisterClass() );
		}
	}

	@Test
	public void testCollectionPersisterSpecified() throws Exception {
		//tests the persister specified by the @Persister annotation on a collection
		if ( isMetadataUsed() ) {
			String expectedRole = Deck.class.getName() + ".cards";
			boolean found = false;
			for ( PluralAttributeBinding attributeBinding : metadata().getCollectionBindings() ) {
				String role = attributeBinding.getAttribute().getRole();
				//tests the persister specified by the @Persister annotation on a collection
				if ( expectedRole.equals( role ) ) {
					assertEquals(
							"Incorrect Persister class for collection " + role, CollectionPersister.class,
							attributeBinding.getExplicitPersisterClass()
					);
					found = true;
					break;
				}
			}
			assertTrue( found );
		}
		else {
			Collection collection = configuration().getCollectionMapping( Deck.class.getName() + ".cards" );
			assertEquals( "Incorrect Persister class for collection " + collection.getRole(), CollectionPersister.class,
					collection.getCollectionPersisterClass() );
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Card.class,
				Deck.class
		};
	}
}
