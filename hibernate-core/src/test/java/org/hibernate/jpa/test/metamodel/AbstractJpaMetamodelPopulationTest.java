/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12871")
public abstract class AbstractJpaMetamodelPopulationTest extends BaseEntityManagerFunctionalTestCase {
	@Entity(name = "SimpleAnnotatedEntity")
	public static class SimpleAnnotatedEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String data;
	}

	@Entity(name = "CompositeIdAnnotatedEntity")
	public static class CompositeIdAnnotatedEntity {
		@EmbeddedId
		private CompositeIdId id;
		private String data;
	}

	@Embeddable
	public static class CompositeIdId implements Serializable {
		private Integer id1;
		private Integer id2;
	}

	@Override
	protected String[] getMappings() {
		return new String[] {
				"org/hibernate/jpa/test/metamodel/SimpleEntity.hbm.xml",
				"org/hibernate/jpa/test/metamodel/CompositeIdEntity.hbm.xml",
				"org/hibernate/jpa/test/metamodel/CompositeId2Entity.hbm.xml"
		};
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleAnnotatedEntity.class, CompositeIdAnnotatedEntity.class };
	}

	protected abstract String getJpaMetamodelPopulationValue();

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.JPA_METAMODEL_POPULATION, getJpaMetamodelPopulationValue() );
	}

	@Test
	public void testMetamodel() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			final Metamodel metamodel = entityManager.getMetamodel();

			if ( getJpaMetamodelPopulationValue().equalsIgnoreCase( "disabled" ) ) {
				// In 5.1, metamodel returned null.
				// In 5.2+, metamodel erturned as a non-null instance.
				assertNotNull( metamodel );
				assertEquals( 0, metamodel.getManagedTypes().size() );
				assertEquals( 0, metamodel.getEntities().size() );
				assertEquals( 0, metamodel.getEmbeddables().size() );
				return;
			}

			assertNotNull( metamodel );

			assertManagedTypes( metamodel );
			assertEntityTypes( metamodel );
			assertEmbeddableTypes( metamodel );
		}
		finally {
			entityManager.close();
		}
	}

	private void assertManagedTypes(Metamodel metamodel) {
		if ( getJpaMetamodelPopulationValue().equalsIgnoreCase( "enabled" ) ) {
			// All managed types should be included, dynamic-map and annotation based.
			// EntityType(SimpleAnnotatedEntity)
			// EntityType(CompositeIdAnnotatedEntity)
			// EntityType(null) - SimpleEntity (dynamic-map entity)
			// EntityType(null) - CompositeIdEntity (dynamic-map entity)
			// EntityType(null) - CompositeId2Entity (dynamic-map entity)
			// EmbeddableType(CompositeIdId) - CompositeIdAnnotatedEntity's EmbeddedId identifier
			// EmbeddableType(Map) - CompositeIdEntity's (dynamic-map entity) identifier
			// EmbeddableType(Map) - CompositeId2Entity's (dynamic-map entity) identifier
			assertEquals( 8, metamodel.getManagedTypes().size() );
		}
		else {
			// When ignoreUnsupported is used, any managed-type that refers to a dynamic-map entity type
			// or a managed type that is owned by said dynamic-map entity type should be excluded.
			// Therefore this collection should only include 3 elements
			// EntityType(SimpleAnnotated)
			// EntityType(CompositeIdAnnotatedEntity)
			// EmbeddableType(CompositeIdId) - CompositeIdAnnotatedEntity's EmbeddedId identifier
			assertEquals( 3, metamodel.getManagedTypes().size() );
		}
	}

	private void assertEntityTypes(Metamodel metamodel) {
		final Set<String> entityNames = metamodel.getEntities().stream()
				.map( EntityType::getName )
				.collect( Collectors.toSet() );

		if ( getJpaMetamodelPopulationValue().equalsIgnoreCase( "enabled" ) ) {
			// Should include all entity types
			// EntityType(SimpleAnnotatedEntity)
			// EntityType(CompositeIdAnnotatedEntity)
			// EntityType(null) - SimpleEntity (dynamic-map entity)
			// EntityType(null) - CompositeIdEntity (dynamic-map entity)
			// EntityType(null) - CompositeId2Entity (dynamic-map entity)
			assertEquals( 5, entityNames.size() );
			assertTrue( entityNames.contains( "SimpleAnnotatedEntity" ) );
			assertTrue( entityNames.contains( "CompositeIdAnnotatedEntity" ) );
			assertTrue( entityNames.contains( "SimpleEntity" ) );
			assertTrue( entityNames.contains( "CompositeIdEntity" ) );
			assertTrue( entityNames.contains( "CompositeId2Entity" ) );
		}
		else {
			// In 5.1, this returns 5 elements
			// CompositeIdAnnotatedEntity
			// SimpleAnnotatedEntity
			// SimpleEntity <-- this should not exist since its entity-type is filtered
			// CompositeIdEntity <-- this should not exist since its entity-type is filtered
			// CompsoiteId2Entity <-- this should not exist since its entity-type is filtered
			//
			// In 5.2, this returns 5 elements too.
			// In 5.3, this returns 5 elements too.
			assertEquals( 2, entityNames.size() );
			assertTrue( entityNames.contains( "SimpleAnnotatedEntity" ) );
			assertTrue( entityNames.contains( "CompositeIdAnnotatedEntity" ) );
		}
	}

	private void assertEmbeddableTypes(Metamodel metamodel) {
		final Set<EmbeddableType<?>> embeddableTypes = metamodel.getEmbeddables();
		if ( getJpaMetamodelPopulationValue().equalsIgnoreCase( "enabled" ) ) {
			// EmbeddableType(CompositeIdId) - CompositeIdAnnotatedEntity's EmbeddedId identifier
			// EmbeddableType(Map) - CompositeIdEntity (dynamic-map entity) identifier
			// EmbeddableType(Map) - CompositeId2Entity (dynamic-map entity) identifier
			assertEquals( 3, embeddableTypes.size() );
		}
		else {
			// This should return only 1 element
			// EmbeddableType(CompositeIdId) - CompositeIdAnnotatedEntity's EmbeddedId identifier
			// The dynamic-map entity type's composite-id embeddable types should be excluded.
			assertEquals( 1, embeddableTypes.size() );
		}
	}
}
