/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.descriptor;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.BasicValue;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		GeneratorDescriptorTests.TheEntity.class,
		GeneratorDescriptorTests.ExportableEntity.class,
		GeneratorDescriptorTests.GenericGeneratorEntity.class,
		GeneratorDescriptorTests.IdentityEntity.class,
		GeneratorDescriptorTests.UuidEntity.class,
		GeneratorDescriptorTests.SequenceEntity.class,
		GeneratorDescriptorTests.TableEntity.class
})
@SessionFactory
@TestMethodOrder(OrderAnnotation.class)
class GeneratorDescriptorTests {
	@Test
	@Order(1)
	void nonExportableGeneratorIsDeferredUntilFactoryConstruction(SessionFactoryScope scope) {
		final var entityBinding = scope.getMetadataImplementor().getEntityBinding( TheEntity.class.getName() );
		final var identifierValue = (BasicValue) entityBinding.getIdentifier();
		final var descriptor = identifierValue.getCustomIdGeneratorCreator();
		final var genericBinding =
				scope.getMetadataImplementor().getEntityBinding( GenericGeneratorEntity.class.getName() );
		final var genericDescriptor =
				((BasicValue) genericBinding.getIdentifier()).getCustomIdGeneratorCreator();

		assertThat( descriptor.getGeneratorClass( null ) ).isEqualTo( NonExportableGenerator.class );
		assertThat( genericDescriptor.getGeneratorClass( null ) )
				.isEqualTo( NonExportableGenericGenerator.class );
		assertThat( NonExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 0 );
		assertThat( NonExportableGenericGenerator.CONSTRUCTION_COUNT ).hasValue( 0 );

		final TheEntity entity = new TheEntity();
		scope.inTransaction( session -> session.persist( entity ) );
		assertThat( NonExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( NonExportableGenericGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( entity.id ).isEqualTo( 1L );
	}

	@Test
	@Order(2)
	void exportableGeneratorIsReusedByRuntimeModel(SessionFactoryScope scope) {
		assertThat( ExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( ExportableGenerator.REGISTERED_INSTANCE ).isNotNull();

		final ExportableEntity entity = new ExportableEntity();
		scope.inTransaction( session -> session.persist( entity ) );

		assertThat( ExportableGenerator.CONSTRUCTION_COUNT ).hasValue( 1 );
		assertThat( ExportableGenerator.GENERATING_INSTANCE )
				.isSameAs( ExportableGenerator.REGISTERED_INSTANCE );
	}

	@Test
	@Order(3)
	void identityGeneratorCarriesItsBootSemantics(SessionFactoryScope scope) {
		final var entityBinding =
				scope.getMetadataImplementor().getEntityBinding( IdentityEntity.class.getName() );
		final var identifierValue = (BasicValue) entityBinding.getIdentifier();
		final var descriptor = identifierValue.getCustomIdGeneratorCreator();

		assertThat( descriptor.getGeneratorClass( null ) )
				.isEqualTo( org.hibernate.id.IdentityGenerator.class );
		assertThat( descriptor.requiresBootPreparation( null ) )
				.as( "boot preparation for %s", descriptor.getClass().getName() )
				.isFalse();
		assertThat( identifierValue.getColumns().get( 0 ).isIdentity() ).isTrue();
	}

	@Test
	@Order(4)
	void uuidGeneratorIsRepresentedDirectly(SessionFactoryScope scope) {
		final var entityBinding =
				scope.getMetadataImplementor().getEntityBinding( UuidEntity.class.getName() );
		final var descriptor =
				((BasicValue) entityBinding.getIdentifier()).getCustomIdGeneratorCreator();

		assertThat( descriptor.getGeneratorClass( null ) )
				.isEqualTo( org.hibernate.id.uuid.UuidGenerator.class );
		assertThat( descriptor.requiresBootPreparation( null ) ).isFalse();
	}

	@Test
	@Order(5)
	void sequenceAndTableGeneratorsAreRepresentedDirectly(SessionFactoryScope scope) {
		final var sequenceBinding =
				scope.getMetadataImplementor().getEntityBinding( SequenceEntity.class.getName() );
		final var sequenceDescriptor =
				((BasicValue) sequenceBinding.getIdentifier()).getCustomIdGeneratorCreator();
		final var tableBinding =
				scope.getMetadataImplementor().getEntityBinding( TableEntity.class.getName() );
		final var tableDescriptor =
				((BasicValue) tableBinding.getIdentifier()).getCustomIdGeneratorCreator();

		assertThat( sequenceDescriptor.getGeneratorClass( null ) )
				.isEqualTo( org.hibernate.id.enhanced.SequenceStyleGenerator.class );
		assertThat( sequenceDescriptor.isExportable( null ) ).isTrue();
		assertThat( tableDescriptor.getGeneratorClass( null ) )
				.isEqualTo( org.hibernate.id.enhanced.TableGenerator.class );
		assertThat( tableDescriptor.isExportable( null ) ).isTrue();

		final var sequenceEntity = new SequenceEntity();
		final var tableEntity = new TableEntity();
		scope.inTransaction( session -> {
			session.persist( sequenceEntity );
			session.persist( tableEntity );
		} );
		assertThat( sequenceEntity.id ).isNotNull();
		assertThat( tableEntity.id ).isNotNull();
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@IdGeneratorType(NonExportableGenerator.class)
	@interface GeneratedId {
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@IdGeneratorType(ExportableGenerator.class)
	@interface ExportedGeneratedId {
	}

	public static class NonExportableGenerator implements BeforeExecutionGenerator {
		private static final AtomicInteger CONSTRUCTION_COUNT = new AtomicInteger();
		private static final AtomicLong VALUE = new AtomicLong();

		public NonExportableGenerator() {
			CONSTRUCTION_COUNT.incrementAndGet();
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return VALUE.incrementAndGet();
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	public static class ExportableGenerator implements BeforeExecutionGenerator, ExportableProducer {
		private static final AtomicInteger CONSTRUCTION_COUNT = new AtomicInteger();
		private static ExportableGenerator REGISTERED_INSTANCE;
		private static ExportableGenerator GENERATING_INSTANCE;

		public ExportableGenerator() {
			CONSTRUCTION_COUNT.incrementAndGet();
		}

		@Override
		public void registerExportables(Database database) {
			REGISTERED_INSTANCE = this;
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			GENERATING_INSTANCE = this;
			return 1L;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	public static class NonExportableGenericGenerator implements IdentifierGenerator {
		private static final AtomicInteger CONSTRUCTION_COUNT = new AtomicInteger();

		public NonExportableGenericGenerator() {
			CONSTRUCTION_COUNT.incrementAndGet();
		}

		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			return 1L;
		}
	}

	@Entity(name = "GeneratorDescriptorEntity")
	static class TheEntity {
		@Id
		@GeneratedId
		Long id;
	}

	@Entity(name = "ExportableGeneratorDescriptorEntity")
	static class ExportableEntity {
		@Id
		@ExportedGeneratedId
		Long id;
	}

	@Entity(name = "GenericGeneratorDescriptorEntity")
	static class GenericGeneratorEntity {
		@Id
		@GenericGenerator(type = NonExportableGenericGenerator.class)
		Long id;
	}

	@Entity(name = "IdentityGeneratorDescriptorEntity")
	static class IdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;
	}

	@Entity(name = "UuidGeneratorDescriptorEntity")
	static class UuidEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		UUID id;
	}

	@Entity(name = "SequenceGeneratorDescriptorEntity")
	static class SequenceEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "descriptor-sequence")
		@SequenceGenerator(
				name = "descriptor-sequence",
				sequenceName = "descriptor_sequence",
				initialValue = 3,
				allocationSize = 7
		)
		Long id;
	}

	@Entity(name = "TableGeneratorDescriptorEntity")
	static class TableEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "descriptor-table")
		@TableGenerator(
				name = "descriptor-table",
				table = "descriptor_generator_table",
				pkColumnName = "generator_name",
				valueColumnName = "generator_value",
				pkColumnValue = "table-entity",
				initialValue = 5,
				allocationSize = 3
		)
		Long id;
	}
}
