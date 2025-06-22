/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import java.time.Instant;
import java.util.Date;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.orm.test.mapping.mutability.attribute.BasicAttributeMutabilityTests;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.descriptor.java.Immutability;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for combining {@link Mutability} and {@link Immutable} with conversions
 * directly on attributes as a baseline for applying {@link Mutability} and {@link Immutable}
 * to the converter class
 *
 * @see BasicAttributeMutabilityTests
 * @see ImmutableConverterTests
 * @see ImmutabilityConverterTests
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ImmutableConvertedBaselineTests.TheEntity.class )
@SessionFactory
public class ImmutableConvertedBaselineTests {
	private static final Instant START = Instant.now();

	/**
	 * Essentially the same as {@link BasicAttributeMutabilityTests#verifyDomainModel}
	 */
	@Test
	void verifyDomainModel(DomainModelScope domainModelScope, SessionFactoryScope sfSessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope.getEntityBinding( TheEntity.class );
		final EntityPersister entityDescriptor = sfSessionFactoryScope
				.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( TheEntity.class );

		// `@Immutable`
		final Property theDateProperty = persistentClass.getProperty( "theDate" );
		assertThat( theDateProperty.isUpdateable() ).isFalse();
		final AttributeMapping theDateAttribute = entityDescriptor.findAttributeMapping( "theDate" );
		assertThat( theDateAttribute.getExposedMutabilityPlan().isMutable() ).isFalse();

		// `@Mutability(Immutability.class)`
		final Property anotherDateProperty = persistentClass.getProperty( "anotherDate" );
		assertThat( anotherDateProperty.isUpdateable() ).isTrue();
		final AttributeMapping anotherDateAttribute = entityDescriptor.findAttributeMapping( "anotherDate" );
		assertThat( anotherDateAttribute.getExposedMutabilityPlan().isMutable() ).isFalse();
	}

	/**
	 * Effectively the same as {@linkplain BasicAttributeMutabilityTests#testImmutableManaged}
	 *
	 * Because it is non-updateable, no UPDATE
	 */
	@Test
	void testImmutableManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Immutable test", Date.from( START ) ) );
		} );

		// load a managed reference and mutate the date
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			theEntity.theDate.setTime( Instant.EPOCH.toEpochMilli() );
		} );

		// reload the entity and verify that the mutation was ignored
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.theDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	/**
	 * Effectively the same as {@linkplain BasicAttributeMutabilityTests#testImmutableDetached}
	 *
	 * Because it is non-updateable, no UPDATE
	 */
	@Test
	void testImmutableMerge(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Immutable test", Date.from( START ) ) );
		} );

		// load a detached reference
		final TheEntity detached = scope.fromTransaction( (session) -> session.find( TheEntity.class, 1 ) );

		// make the change to the detached instance and merge it
		detached.theDate.setTime( Instant.EPOCH.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );

		// verify the value did not change
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.theDate.getTime() ).isEqualTo( START.toEpochMilli() );
		} );
	}

	/**
	 * Effectively the same as {@linkplain BasicAttributeMutabilityTests#testImmutabilityManaged}.
	 *
	 * Because the state mutation is done on a managed instance, Hibernate detects that; and
	 * because it is internal-state-immutable, we will ignore the mutation and there will
	 * be no UPDATE
	 */
	@Test
	void testImmutabilityManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Mutability test", Date.from( START ) ) );
		} );

		// try to update the managed form
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate ).isEqualTo( Date.from( START ) );
			theEntity.anotherDate.setTime( Instant.EPOCH.toEpochMilli() );
		} );

		// reload it and verify the value did not change
		final TheEntity detached = scope.fromTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate ).isEqualTo( Date.from( START ) );
			return theEntity;
		} );

		// Unfortunately, dues to how merge works (find + set) this change to the
		// detached instance looks like a set when applied to the managed instance.
		// Therefore, from the perspective of the merge operation, the Date itself was
		// set rather than its internal state being changed.  AKA, this will "correctly"
		// result in an update
		detached.anotherDate.setTime( Instant.EPOCH.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );

		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate ).isEqualTo( Date.from( Instant.EPOCH ) );
		} );
	}

	/**
	 * Effectively the same as {@linkplain BasicAttributeMutabilityTests#testImmutabilityDetached}
	 *
	 * There will be an UPDATE because we cannot distinguish one type of change from another while detached
	 */
	@Test
	void testImmutabilityDetached(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( 1, "@Mutability test", Date.from( START ) ) );
		} );

		// load a detached reference
		final TheEntity detached = scope.fromTransaction( (session) -> session.find( TheEntity.class, 1 ) );

		// mutate the date and merge the detached ref
		detached.anotherDate.setTime( Instant.EPOCH.toEpochMilli() );
		scope.inTransaction( (session) -> session.merge( detached ) );

		// verify the value change was persisted
		scope.inTransaction( (session) -> {
			final TheEntity theEntity = session.find( TheEntity.class, 1 );
			assertThat( theEntity.anotherDate.getTime() ).isEqualTo( Instant.EPOCH.toEpochMilli() );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "TheEntity" )
	@Table( name = "TheEntity" )
	public static class TheEntity {
		@Id
		private Integer id;

		@Basic
		private String name;

		@Immutable
		@Convert(converter = DateConverter.class)
		private Date theDate;

		@Mutability(Immutability.class)
		@Convert(converter = DateConverter.class)
		private Date anotherDate;


		private TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name, Date aDate) {
			this.id = id;
			this.name = name;
			this.theDate = aDate;
			this.anotherDate = aDate;
		}
	}
}
