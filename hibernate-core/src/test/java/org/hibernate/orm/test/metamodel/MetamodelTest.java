/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.metamodel;

import java.util.Arrays;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Jpa(
		annotatedClasses = {
				MetamodelTest.EntityWithCollection.class,
				MetamodelTest.ElementOfCollection.class,
				MetamodelTest.EntityWithCollection2.class,
				MetamodelTest.ElementOfCollection2.class
		}
)
public class MetamodelTest {

	@Test
	@JiraKey(value = "HHH-12906")
	public void testGetAllCollectionRoles(EntityManagerFactoryScope scope) {
		String[] collectionRoles = ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getMetamodel()
				.getAllCollectionRoles();
		Arrays.sort( collectionRoles );
		assertArrayEquals( collectionRoles, new String[] {
				EntityWithCollection.class.getName() + ".collection",
				EntityWithCollection2.class.getName() + ".collection2"
		} );
	}

	@Test
	public void testGetCollectionRolesByEntityParticipant(EntityManagerFactoryScope scope) {
		Set<String> collectionRolesByEntityParticipant = ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getMappingMetamodel()
				.getCollectionRolesByEntityParticipant( ElementOfCollection.class.getName() );
		assertEquals( 1, collectionRolesByEntityParticipant.size() );
		assertEquals(
				EntityWithCollection.class.getName() + ".collection",
				collectionRolesByEntityParticipant.iterator().next()
		);
	}

	@Test
	public void testEntityNames(EntityManagerFactoryScope scope) {
		String[] entityNames = ( (SessionFactoryImplementor) scope.getEntityManagerFactory() ).getMetamodel()
				.getAllEntityNames();
		Arrays.sort( entityNames );
		assertArrayEquals(
				entityNames,
				new String[] {
						ElementOfCollection.class.getName(), ElementOfCollection2.class.getName(),
						EntityWithCollection.class.getName(), EntityWithCollection2.class.getName()
				}
		);
	}

	@Entity(name = "EntityWithCollection")
	public static class EntityWithCollection {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany
		private Set<ElementOfCollection> collection;
	}

	@Entity(name = "ElementOfCollection")
	public static class ElementOfCollection {

		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "EntityWithCollection2")
	public static class EntityWithCollection2 {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany
		private Set<ElementOfCollection2> collection2;
	}

	@Entity(name = "ElementOfCollection2")
	public static class ElementOfCollection2 {

		@Id
		@GeneratedValue
		private Long id;
	}
}
