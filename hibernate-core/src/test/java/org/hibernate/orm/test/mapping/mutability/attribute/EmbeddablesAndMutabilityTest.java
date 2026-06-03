/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mutability.attribute;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Mutability;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = EmbeddablesAndMutabilityTest.TheEntity.class)
@SessionFactory
public class EmbeddablesAndMutabilityTest {

	@Test
	@DisplayName("testing if embedded record property is always immutable since records are immutable by its nature")
	public void embeddedRecordShouldAlwaysBeImmutable(DomainModelScope domainModelScope, SessionFactoryScope sfSessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope
				.getDomainModel()
				.getEntityBinding( TheEntity.class.getName() );
		final EntityPersister entityDescriptor = sfSessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( TheEntity.class );

		final Property property = persistentClass.getProperty( "embeddedRecord" );
		assertTrue( property.isUpdatable() );
		final AttributeMapping attribute = entityDescriptor.findAttributeMapping( "embeddedRecord" );
		final MutabilityPlan<?> mutabilityPlan = attribute.getExposedMutabilityPlan();
		assertFalse( mutabilityPlan.isMutable(),
				"Embedded record property (or any other record property) should be immutable" );
	}

	@Test
	@DisplayName(
			"Testing if embedded mutable class property is always immutable or only when explictely marked as immutable")
	public void embeddedMutableClassPropertyNotAnnotated(DomainModelScope domainModelScope, SessionFactoryScope sfSessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope
				.getDomainModel()
				.getEntityBinding( TheEntity.class.getName() );
		final EntityPersister entityDescriptor = sfSessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( TheEntity.class );

		final Property property = persistentClass.getProperty( "embeddedClass" );
		assertTrue( property.isUpdatable() );
		final AttributeMapping attribute = entityDescriptor.findAttributeMapping( "embeddedClass" );
		final MutabilityPlan<?> mutabilityPlan = attribute.getExposedMutabilityPlan();
		assertTrue( mutabilityPlan.isMutable(),
				"This should fail if embedded mutable class property must be immutable!" );
		assertFalse( mutabilityPlan.isMutable(),
				"This must not fail if embedded mutable class property must be immutable!" );
	}

	@Test
	@DisplayName(
			"Testing if @Mutability annotation should can be used on embedded mutable class property or ignored (or even causing boot failure)")
	public void embeddedMutableClassPropertyAnnotatedWithMutability(DomainModelScope domainModelScope, SessionFactoryScope sfSessionFactoryScope) {
		final PersistentClass persistentClass = domainModelScope
				.getDomainModel()
				.getEntityBinding( TheEntity.class.getName() );
		final EntityPersister entityDescriptor = sfSessionFactoryScope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( TheEntity.class );

		final Property property = persistentClass.getProperty( "embeddedClassWithAnnotation" );
		assertTrue( property.isUpdatable() );
		final AttributeMapping attribute = entityDescriptor.findAttributeMapping( "embeddedClassWithAnnotation" );
		final MutabilityPlan<?> mutabilityPlan = attribute.getExposedMutabilityPlan();
		assertTrue( mutabilityPlan.isMutable(),
				"This should fail if @Mutability annotation should apply to embedded mutable class!" );
		assertFalse( mutabilityPlan.isMutable(),
				"This must not fail if @Mutability annotation should apply to embedded mutable class" );
	}

	@Test
	@DisplayName(
			"Testing if internal state of embedded mutable class can be changed if not explictely marked as immutable")
	public void changingEmbeddedMutableClassPropertyNotAnnotated(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final var entity = new TheEntity();
			entity.id = 1;
			entity.embeddedClass = new EmbeddedClass( "uno", "due" );
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final var theEntity = session.find( TheEntity.class, 1 );
			theEntity.embeddedClass.two = "deux";
		} );

		// reload it and verify the value did not change
		scope.inTransaction( (session) -> {
			final var theEntity = session.find( TheEntity.class, 1 );
			assertEquals( "deux", theEntity.embeddedClass.two,
					"This should fail if embedded mutable class property must be immutable!" );
			assertEquals( "due", theEntity.embeddedClass.two,
					"This must not fail if embedded mutable class property must be immutable!" );
		} );
	}

	@Entity(name = "TheEntity")
	@Table(name = "TheEntity")
	public static class TheEntity {

		@Id
		private Integer id;

		@Embedded
		private EmbeddedRecord embeddedRecord;

		@Embedded
		private EmbeddedClass embeddedClass;

		@Embedded
		@AttributeOverride(name = "one", column = @Column(name = "eins"))
		@AttributeOverride(name = "two", column = @Column(name = "zwei"))
		// If @Embedded and @Mutablity can not be both preset at same time, this should cause boot failure
		@Mutability(MyMutablilityPlan.class)
		private EmbeddedClass embeddedClassWithAnnotation;

		private TheEntity() {
		}
	}

	public static class EmbeddedClass {

		private String one;
		private String two;

		public EmbeddedClass() {
		}

		public EmbeddedClass(String one, String two) {
			this.one = one;
			this.two = two;
		}
	}

	public record EmbeddedRecord(String three, String four) {
	}

	public static class MyMutablilityPlan extends MutableMutabilityPlan<EmbeddedClass> {
		@Override
		protected EmbeddedClass deepCopyNotNull(EmbeddedClass value) {
			return new EmbeddedClass( value.one, value.two );
		}
	}
}
