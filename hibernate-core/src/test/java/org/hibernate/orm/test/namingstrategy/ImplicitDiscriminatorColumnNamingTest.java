/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.DiscriminatorColumnNamingInput;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.MappingSettings.IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;
import static org.hibernate.cfg.MappingSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS;

/// Entity discriminator naming, annotation-default provenance, and runtime coverage.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitDiscriminatorColumnNamingTest {
	@Test
	void suppliedAnnotationDefaultsBypassImplicitNaming() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Strategy();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Defaulted.class, LengthOnly.class, ExplicitDefault.class, Explicit.class, Empty.class ),
					implicit, physical );
			assertThat( implicit.inputs ).singleElement().satisfies( input ->
					assertThat( input.entity().getClassName() ).isEqualTo( Empty.class.getName() ) );
			for (var type : List.of( Defaulted.class, LengthOnly.class, ExplicitDefault.class )) {
				assertThat( metadata.getEntityBinding( type.getName() ).getDiscriminator().getColumns().get( 0 ).getName() )
						.isEqualTo( "p_DTYPE" );
			}
			final var length = metadata.getEntityBinding( LengthOnly.class.getName() ).getDiscriminator().getColumns().get( 0 );
			assertThat( length.getLength() ).isEqualTo( 64 );
			final var explicit = metadata.getEntityBinding( Explicit.class.getName() ).getDiscriminator().getColumns().get( 0 );
			assertThat( explicit.getName() ).isEqualTo( "p_kind" );
			assertThat( explicit.isQuoted() ).isTrue();
			assertThat( explicit.getLength() ).isEqualTo( 1 );
			assertThat( explicit.getSqlType() ).isEqualTo( "char(1)" );
			// The existing discriminator source adapter does not expose annotation comments.
			assertThat( explicit.getComment() ).isNull();
			assertThat( explicit.getOptions() ).isEqualTo( "custom_options" );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "DTYPE" ) )
					.hasSize( 3 ).allSatisfy( name -> assertThat( name.isExplicit() ).isTrue() );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "dtype_custom" ) )
					.singleElement().satisfies( name -> assertThat( name.isExplicit() ).isFalse() );
		}
	}

	@Test
	void xmlDefaultsAreSuppliedAndGlobalQuotingIsPreserved() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.globally_quoted_identifiers", true ).build()) {
			final var implicit = new Strategy();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addMappingResource( "org/hibernate/orm/test/namingstrategy/discriminator-naming.orm.xml" ),
					implicit, physical );
			assertThat( implicit.inputs ).isEmpty();
			final var column = metadata.getEntityBinding( XmlDefault.class.getName() ).getDiscriminator().getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_DTYPE" );
			assertThat( column.isQuoted() ).isTrue();
			assertThat( column.getLength() ).isEqualTo( 64 );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "DTYPE" ) ).singleElement()
					.satisfies( name -> assertThat( name.isExplicit() ).isTrue() );
		}
	}

	@Test
	void indexResolvesTheLogicalDiscriminatorName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Indexed.class ), new Strategy(), new Physical() );
			final var entity = metadata.getEntityBinding( Indexed.class.getName() );
			final var table = (org.hibernate.mapping.PhysicalTable) entity.getTable();
			assertThat( table.getIndexes().get( "discriminator_lookup" ).getSelectables() )
					.containsExactly( entity.getDiscriminator().getColumns().get( 0 ) );
		}
	}

	@Test
	void generatedQuotedNameWorksAtRuntime() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" ).build()) {
			final var implicit = new Strategy();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Root.class, Child.class ), implicit, physical );
			assertThat( implicit.inputs ).singleElement().satisfies( input -> {
				assertThat( input.entity().getClassName() ).isEqualTo( Root.class.getName() );
				assertThat( input.entity().getJpaEntityName() ).isEqualTo( "DiscriminatorRoot" );
			} );
			final var column = metadata.getEntityBinding( Root.class.getName() ).getDiscriminator().getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_dtype_custom" );
			assertThat( column.isQuoted() ).isTrue();
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "dtype_custom" ) ).hasSize( 1 );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var child = new Child();
					child.id = 1;
					session.persist( child );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					assertThat( session.createQuery( "from DiscriminatorRoot", Root.class ).getResultList() )
							.singleElement().isInstanceOf( Child.class );
				}
			}
		}
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void joinedCreationRulesArePreserved(boolean enabled) {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( IMPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, enabled ).build()) {
			final var implicit = new Strategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Joined.class, JoinedChild.class ), implicit, new Physical() );
			assertThat( implicit.inputs ).hasSize( enabled ? 1 : 0 );
			final var discriminator = metadata.getEntityBinding( Joined.class.getName() ).getDiscriminator();
			if ( enabled ) {
				assertThat( discriminator.getColumns().get( 0 ).getName() ).isEqualTo( "p_dtype_custom" );
			}
			else {
				assertThat( discriminator ).isNull();
			}
		}
	}

	@Test
	void suppressedColumnsAndFormulasDoNotInvokeNaming() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, true ).build()) {
			final var implicit = new Strategy();
			final var physical = new Physical();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Standalone.class, FormulaRoot.class, FormulaChild.class,
							Concrete.class, ConcreteChild.class, ExplicitJoined.class, ExplicitJoinedChild.class ), implicit, physical );
			assertThat( implicit.inputs ).isEmpty();
			assertThat( physical.inputs ).allSatisfy( name -> assertThat( name.getText() ).isEqualTo( "id" ) );
			assertThat( metadata.getEntityBinding( FormulaRoot.class.getName() ).getDiscriminator().hasFormula() ).isTrue();
		}
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void invalidResultsAreRejected(boolean returnNull) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry,
					new MappingSources().addManagedClasses( Root.class, Child.class ), new StandardImplicitNamingStrategy() {
						@Override
						public LogicalName determineDiscriminatorColumnName(DiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
							return returnNull ? null : new LogicalName( "invalid", false, true );
						}
					} ) ).hasMessageContaining( "non-null implicit name for entity discriminator column" );
		}
	}

	@Entity @DiscriminatorColumn(name = "")
	@Table(indexes = @Index(name = "discriminator_lookup", columnList = "`dtype_custom`"))
	static class Indexed { @Id long id; }

	static class XmlDefault { long id; }

	static class Strategy extends StandardImplicitNamingStrategy {
		final List<DiscriminatorColumnNamingInput> inputs = new ArrayList<>();
		@Override
		public LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context) {
			throw new AssertionError( "Discriminator naming must not invoke basic-column naming" );
		}
		@Override
		public LogicalName determineDiscriminatorColumnName(DiscriminatorColumnNamingInput input, ImplicitNamingContext context) {
			inputs.add( input );
			return context.implicitName( "dtype_custom", true );
		}
	}
	static class Physical extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}
	@Entity(name = "DiscriminatorRoot") @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	static class Root { @Id long id; }
	@Entity @DiscriminatorValue("child") static class Child extends Root {}
	@Entity @DiscriminatorColumn static class Defaulted { @Id long id; }
	@Entity @DiscriminatorColumn(length = 64) static class LengthOnly { @Id long id; }
	@Entity @DiscriminatorColumn(name = "DTYPE") static class ExplicitDefault { @Id long id; }
	@Entity @DiscriminatorColumn(name = "`kind`", discriminatorType = DiscriminatorType.CHAR,
			columnDefinition = "char(1)", comment = "kind comment", options = "custom_options")
	@DiscriminatorValue("E")
	static class Explicit { @Id long id; }
	@Entity @DiscriminatorColumn(name = "") static class Empty { @Id long id; }
	@Entity @Inheritance(strategy = InheritanceType.JOINED) static class Joined { @Id long id; }
	@Entity static class JoinedChild extends Joined {}
	@Entity static class Standalone { @Id long id; }
	@Entity @DiscriminatorFormula("'root'") static class FormulaRoot { @Id long id; }
	@Entity static class FormulaChild extends FormulaRoot {}
	@Entity @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS) static class Concrete { @Id long id; }
	@Entity static class ConcreteChild extends Concrete {}
	@Entity @Inheritance(strategy = InheritanceType.JOINED) @DiscriminatorColumn
	static class ExplicitJoined { @Id long id; }
	@Entity static class ExplicitJoinedChild extends ExplicitJoined {}
}
