/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import java.util.Map;

import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.orm.test.mapping.mutability.converted.MapConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.Immutability;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @implNote Uses a converter just for helping with the Map part.  It is the
 * {@link Mutability} usage that is important
 *
 * @author Steve Ebersole
 */
@JiraKey( "HHH-16081" )
@DomainModel( annotatedClasses = ImmutabilityMapAsBasicTests.TestEntity.class )
@SessionFactory( useCollectingStatementInspector = true )
public class ImmutabilityMapAsBasicTests {
	@Test
	void verifyMetamodel(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		domainModelScope.withHierarchy( TestEntity.class, (entity) -> {
			final Property property = entity.getProperty( "data" );
			assertThat( property.isUpdatable() ).isTrue();

			final BasicValue value = (BasicValue) property.getValue();
			final BasicValue.Resolution<?> resolution = value.resolve();
			assertThat( resolution.getMutabilityPlan().isMutable() ).isFalse();
		} );

		final MappingMetamodelImplementor mappingMetamodel = sessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.getEntityDescriptor( TestEntity.class );
		final AttributeMapping attribute = entityDescriptor.findAttributeMapping( "data" );
		assertThat( attribute.getAttributeMetadata().isUpdatable() ).isTrue();
		assertThat( attribute.getExposedMutabilityPlan().isMutable() ).isFalse();
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testDirtyCheckingManaged(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// mutate the managed entity state - should not trigger update since it is immutable
		scope.inTransaction( (session) -> {
			// load a managed reference
			final TestEntity managed = session.get( TestEntity.class, 1 );
			assertThat( managed.data ).hasSize( 2 );

			// make the change
			managed.data.put( "ghi", "789" );

			// clear statements prior to flush
			statementInspector.clear();
		} );

		assertThat( statementInspector.getSqlQueries() ).isEmpty();
	}

	/**
	 * Illustrates how we can't really detect that an "internal state" mutation
	 * happened while detached.  When merged, we just see a different value.
	 */
	@Test
	@JiraKey( "HHH-16132" )
	void testDirtyCheckingMerge(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// load a detached reference
		final TestEntity detached = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( detached.data ).hasSize( 2 );

		// make the change
		detached.data.put( "jkl", "007" );

		// clear statements prior to merge
		statementInspector.clear();

		// do the merge
		scope.inTransaction( (session) -> session.merge( detached ) );

		// the SELECT + UPDATE
		assertThat( statementInspector.getSqlQueries() ).hasSize( 2 );
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testNotDirtyCheckingManaged(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// make no changes to a managed entity
		scope.inTransaction( (session) -> {
			// Load a managed reference
			final TestEntity managed = session.get( TestEntity.class, 1 );
			assertThat( managed.data ).hasSize( 2 );

			// make no changes

			// clear statements in prep for next check
			statementInspector.clear();
		} );

		// because we made no changes, there should be no update
		assertThat( statementInspector.getSqlQueries() ).isEmpty();
	}

	@Test
	@JiraKey( "HHH-16132" )
	void testNotDirtyCheckingMerge(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();

		// load a detached instance
		final TestEntity detached = scope.fromTransaction( (session) -> session.get( TestEntity.class, 1 ) );
		assertThat( detached.data ).hasSize( 2 );

		// clear statements in prep for next check
		statementInspector.clear();

		// merge the detached reference without making any changes
		scope.inTransaction( (session) -> session.merge( detached ) );
		// (the SELECT) - should not trigger the UPDATE
		assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TestEntity(
					1,
					Map.of(
							"abc", "123",
							"def", "456"
					)
			) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TestEntity" )
	@Table( name = "entity_mutable_map" )
	public static class TestEntity {
		@Id
		private Integer id;

		@Mutability(Immutability.class)
		@Convert( converter = MapConverter.class )
		private Map<String,String> data;

		private TestEntity() {
			// for use by Hibernate
		}

		public TestEntity(Integer id, Map<String,String> data) {
			this.id = id;
			this.data = data;
		}
	}
}
