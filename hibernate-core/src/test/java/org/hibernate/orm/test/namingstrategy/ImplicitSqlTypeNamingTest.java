/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.spi.*;
import org.hibernate.boot.model.process.internal.NamedSqlTypeNames;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.namingstrategy.NamedSqlTypeNamingImpactTest.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

/// Exercises implicit SQL type naming independently from physical spelling and SQL rendering.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitSqlTypeNamingTest {
	/// Verify that a named enum is resolved before its containing Oracle array, so the
	/// array strategy can choose between the element’s logical and physical names.
	/// The composed array name must reach column SQL, helper create/drop DDL, and JDBC lookup.
	@Test
	void enumThenArrayReceiveDistinctNamingDecisions() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect(), naming, EnumArrayEntity.class, metadata -> {
			assertThat( naming.events ).containsExactly( "enum:State", "array:State" );
			assertThat( naming.arrays.get( 0 ).namedElement().orElseThrow().name().logicalName().getText() ).isEqualTo( "e_State" );
			assertThat( naming.arrays.get( 0 ).namedElement().orElseThrow().name().physicalName().getText() ).isEqualTo( "p_e_State" );
			assertThat( columnType( metadata, EnumArrayEntity.class, "states" ) ).isEqualTo( "p_a_e_State" );
			final var database = metadata.getDatabase();
			final var context = org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.forTests( database.getJdbcEnvironment() );
			final var udt = database.getDefaultNamespace().getUserDefinedTypes().iterator().next();
			assertThat( String.join( " ", database.getDialect().getUserDefinedTypeExporter().getSqlCreateStrings( udt, metadata, context ) ) )
					.contains( "p_a_e_State", "p_a_e_State_length" );
			assertThat( String.join( " ", database.getDialect().getUserDefinedTypeExporter().getSqlDropStrings( udt, metadata, context ) ) )
					.contains( "p_a_e_State", "p_a_e_State_length" );
			final var type = (BasicValue) metadata.getEntityBinding( EnumArrayEntity.class.getName() ).getProperty( "states" ).getValue();
			assertThat( ((org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType) type.resolve().getJdbcType()).getSqlTypeName() ).isEqualTo( "p_a_e_State" );
			final var reused = database.getTypeConfiguration().getJdbcTypeRegistry().findSqlTypedDescriptor( "p_a_e_State" );
			assertThat( reused.getSqlTypeName() ).isEqualTo( "p_a_e_State" );


		} );
	}

	/// Keep PostgreSQL’s anonymous array syntax outside array naming while applying
	/// both naming stages to its independently named enum element.
	@Test
	void anonymousArrayOnlyNamesItsEnum() {
		final var naming = new Naming();
		inspect( new PostgreSQLDialect(), naming, EnumArrayEntity.class, metadata -> {
			assertThat( naming.events ).containsExactly( "enum:State" );
			assertThat( columnType( metadata, EnumArrayEntity.class, "states" ) ).isEqualTo( "p_e_State array" );
		} );
	}

	/// Ensure enum naming receives the effective converted database labels and converter
	/// identity, rather than having to infer them from Java enum constants.
	@Test
	void convertedLabelsAreAvailableBeforeNaming() {
		final var naming = new Naming();
		inspect( new PostgreSQLDialect(), naming, ConvertedEnumEntity.class, metadata -> {
			assertThat( naming.enums ).hasSize( 1 );
			assertThat( naming.enums.get( 0 ).values() ).containsExactly( "A", "I" );
			assertThat( naming.enums.get( 0 ).converterType() ).isPresent();
			assertThat( columnType( metadata, ConvertedEnumEntity.class, "state" ) ).isEqualTo( "p_e_Code" );
		} );
	}

	/// Treat repeated mappings of the same enum as one shared naming decision, ensuring
	/// both columns use the same finalized name without invoking the strategy again.
	@Test
	void repeatedEnumRequestsReuseTheDecision() {
		final var naming = new Naming();
		inspect( new PostgreSQLDialect(), naming, RepeatedEnumEntity.class, metadata -> {
			assertThat( naming.enums ).hasSize( 1 );
			assertThat( columnType( metadata, RepeatedEnumEntity.class, "first" ) ).isEqualTo( "p_e_State" );
			assertThat( columnType( metadata, RepeatedEnumEntity.class, "second" ) ).isEqualTo( "p_e_State" );
		} );
	}

	/// Allow an array strategy to inspect an explicitly named struct’s declaration and
	/// finalized physical name, without introducing an implicit struct naming decision.
	@Test
	void structDependencyIsAvailableWithoutImplicitStructNaming() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect(), naming, StructArrayEntity.class, metadata -> {
			assertThat( naming.enums ).isEmpty();
			assertThat( naming.arrays ).hasSize( 1 );
			final var array = naming.arrays.get( 0 );
			assertThat( array.declaredElementTypeName().orElseThrow().getText() ).isEqualTo( "Piece" );
			assertThat( array.namedElement().orElseThrow().kind() ).isEqualTo( NamedSqlTypeKind.STRUCT );
			assertThat( array.namedElement().orElseThrow().name().physicalName().getText() ).isEqualTo( "p_Piece" );
		} );
	}

	/// Ensure a strategy can distinguish Oracle nested tables from VARRAY objects,
	/// even though the built-in default name convention is the same for both.
	@Test
	void nestedTableRepresentationIsExposed() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect(), naming, NestedTableEntity.class, metadata ->
				assertThat( naming.arrays.get( 0 ).representation() ).isEqualTo( ArrayNamingRepresentation.NESTED_TABLE ) );
	}

	/// Establish that the implicit strategy owns mapped array object naming: an override
	/// of the dialect’s array type rendering method must not replace the selected name.
	@Test
	void customDialectCannotReplaceImplicitArrayNaming() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect() {
			@Override public String getArrayTypeName(String javaName, String sqlName, Integer length) {
				return "DialectShouldNotNameThis";
			}
		}, naming, ArrayEntity.class, metadata ->
				assertThat( columnType( metadata, ArrayEntity.class, "numbers" ) ).isEqualTo( "p_a_Integer" ) );
	}

	/// Enforce the enum callback’s non-null, implicit-provenance result contract and
	/// require failure diagnostics to identify the offending callback.
	@Test
	void nullAndExplicitResultsAreRejected() {
		for ( var result : new LogicalName[] { null, new LogicalName( "wrong", false, true ) } ) {
			final var naming = new StandardImplicitNamingStrategy() {
				@Override @Nonnull public LogicalName determineEnumName(@Nonnull EnumNamingInput input, @Nonnull ImplicitNamingContext context) { return result; }
			};
			assertThatThrownBy( () -> inspect( new PostgreSQLDialect(), naming, EnumEntity.class, metadata -> {} ) )
					.hasStackTraceContaining( "determineEnumName" );
		}
	}

	/// Enforce the same non-null, implicit-provenance result contract for array naming,
	/// with diagnostics identifying the array callback.
	@Test
	void invalidArrayResultsAreRejected() {
		for ( var result : new LogicalName[] { null, new LogicalName( "wrong", false, true ) } ) {
			final var naming = new StandardImplicitNamingStrategy() {
				@Override @Nonnull public LogicalName determineArrayName(@Nonnull ArrayNamingInput input, @Nonnull ImplicitNamingContext context) { return result; }
			};
			assertThatThrownBy( () -> inspect( new OracleTypesDialect(), naming, ArrayEntity.class, metadata -> {} ) )
					.hasStackTraceContaining( "determineArrayName" );
		}
	}

	/// Keep an element’s schema separate from its logical and physical object names,
	/// while preserving physical quoting when the resulting array name is rendered.
	@Test
	void qualifiedDependenciesKeepQualificationOutsideTheIdentifier() {
		final var naming = new Naming();
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", new OracleTypesDialect() )
				.applySetting( "hibernate.default_schema", "TypeSchema" )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.type.prefer_native_enum_types", true ).build() ) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( EnumArrayEntity.class ), naming, new TypeNaming( true, true ) );
			final var dependency = naming.arrays.get( 0 ).namedElement().orElseThrow();
			assertThat( dependency.name().logicalName().getText() ).isEqualTo( "e_State" );
			assertThat( dependency.name().physicalName().getText() ).isEqualTo( "p_e_State" );
			assertThat( dependency.name().physicalName().isQuoted() ).isTrue();
			assertThat( dependency.schema().orElseThrow().logicalName().getText() ).isEqualTo( "TypeSchema" );
			assertThat( dependency.schema().orElseThrow().physicalName().getText() ).isEqualTo( "TypeSchema" );
			assertThat( columnType( metadata, EnumArrayEntity.class, "states" ) ).isEqualTo( "TypeSchema.\"p_a_e_State\"" );
		}
	}

	/// Verify that metadata restoration retains the selected enum and array names
	/// without replaying implicit naming, which could otherwise produce different names.
	@Test
	void finalizedMetadataDoesNotReplayImplicitNaming() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect(), naming, EnumArrayEntity.class, metadata -> {
			try {
				final var out = new java.io.ByteArrayOutputStream();
				org.hibernate.boot.serial.MetadataSerialization.serialize( (org.hibernate.boot.spi.MetadataImplementor) metadata ).writeTo( out );
				naming.forbidden = true;
				final var restored = org.hibernate.boot.serial.MetadataSerialization.read( new java.io.ByteArrayInputStream( out.toByteArray() ) )
						.restore( (org.hibernate.boot.registry.StandardServiceRegistry) metadata.getDatabase().getServiceRegistry() ).getMetadata();
				assertThat( columnType( restored, EnumArrayEntity.class, "states" ) ).isEqualTo( "p_a_e_State" );
			}
			catch (RuntimeException e) { throw new AssertionError( e ); }
		} );
	}

	/// Protect the existing scalar, converter, nested Java-array, and differing JDBC
	/// representation name recipes across every built-in implicit strategy.
	@Test
	void builtInDefaultsPreserveEachArrayRecipe() {
		final var context = mock( ImplicitNamingContext.class, CALLS_REAL_METHODS );
		final List<ImplicitNamingStrategy> strategies = List.of( new StandardImplicitNamingStrategy(),
				new ImplicitNamingStrategyJpaCompliantImpl(),
				new org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl(),
				new org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl(),
				new org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl() );
		for ( var strategy : strategies ) {
			assertThat( strategy.determineArrayName( input( Integer.class, Integer.class, null ), context ).getText() ).isEqualTo( "IntegerArray" );
			assertThat( strategy.determineArrayName( input( Integer.class, null, null ), context ).getText() ).isEqualTo( "IntegerArray" );
			assertThat( strategy.determineArrayName( input( Integer[].class, null, null ), context ).getText() ).isEqualTo( "IntegerArrayArray" );
			assertThat( strategy.determineArrayName( input( String.class, Integer.class, null ), context ).getText() ).isEqualTo( "StringIntegerArray" );
			assertThat( strategy.determineArrayName( input( String.class, byte[].class, null ), context ).getText() ).isEqualTo( "StringbyteArrayArray" );
			assertThat( strategy.determineArrayName( input( Integer.class, Integer.class, StateConverter.class ), context ).getText() ).isEqualTo( "StateConverterArray" );
		}
	}

	/// Ensure qualified Java identities let a custom strategy distinguish enums sharing
	/// a simple name, with the same outcome in either entity registration order.
	@Test
	void sameSimpleNameEnumsAreDistinctAndOrderIndependent() {
		for ( boolean reverse : new boolean[] { false, true } ) {
			final var naming = new StandardImplicitNamingStrategy() {
				@Override @Nonnull public LogicalName determineEnumName(@Nonnull EnumNamingInput input, @Nonnull ImplicitNamingContext context) {
					return context.implicitName( input.javaType().qualifiedName().contains( "$First$" ) ? "first_state" : "second_state" );
				}
			};
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( "hibernate.dialect", new PostgreSQLDialect() )
					.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false ).build() ) {
				final var sources = new MappingSources().addManagedClass( reverse ? SecondEntity.class : FirstEntity.class )
						.addManagedClass( reverse ? FirstEntity.class : SecondEntity.class );
				final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources, naming, new TypeNaming( true ) );
				assertThat( columnType( metadata, FirstEntity.class, "state" ) ).isEqualTo( "p_first_state" );
				assertThat( columnType( metadata, SecondEntity.class, "state" ) ).isEqualTo( "p_second_state" );
			}
		}
	}

	/// Keep an enum stored in an ordinary VARCHAR column outside named SQL enum naming,
	/// since that mapping does not introduce an independently named enum object.
	@Test
	void scalarEnumDoesNotInvokeNamedEnumCallback() {
		final var naming = new Naming();
		inspect( new PostgreSQLDialect(), naming, ScalarEnumEntity.class, metadata -> assertThat( naming.enums ).isEmpty() );
	}

	/// Honor an already-physical array name supplied by a custom JDBC descriptor.
	/// Neither naming stage may change it, and column SQL and type registration must agree.
	@Test
	void physicalCustomDescriptorBypassesBothStrategies() {
		final var naming = new Naming();
		inspect( new OracleTypesDialect(), naming, PhysicalArrayEntity.class, metadata -> {
			assertThat( naming.arrays ).isEmpty();
			assertThat( columnType( metadata, PhysicalArrayEntity.class, "numbers" ) ).isEqualTo( "ExistingArray" );
			assertThat( metadata.getDatabase().getDefaultNamespace().getUserDefinedTypes() )
					.extracting( org.hibernate.mapping.UserDefinedType::getName ).containsExactly( "ExistingArray" );
		} );
	}

	static class First { enum State { ON, OFF } }
	static class Second { enum State { ON, OFF } }

	@Entity
	static class FirstEntity {
		@Id long id;
		@JdbcTypeCode(SqlTypes.NAMED_ENUM)
		First.State state;
	}

	@Entity
	static class SecondEntity {
		@Id long id;
		@JdbcTypeCode(SqlTypes.NAMED_ENUM)
		Second.State state;
	}

	@Entity
	static class ScalarEnumEntity {
		@Id long id;
		@JdbcTypeCode(SqlTypes.VARCHAR)
		First.State state;
	}

	public static class PhysicalArrayType extends org.hibernate.dialect.type.internal.OracleArrayJdbcType {
		public PhysicalArrayType() { super( org.hibernate.type.descriptor.jdbc.IntegerJdbcType.INSTANCE, "ExistingArray" ); }
	}

	@Entity
	static class PhysicalArrayEntity {
		@Id long id;
		@org.hibernate.annotations.JdbcType(PhysicalArrayType.class) Integer[] numbers;
	}

	private static ArrayNamingInput input(Class<?> element, Class<?> preferred, Class<?> converter) {
		return new ArrayNamingInput( NamedSqlTypeNames.javaType( element ), Optional.ofNullable( preferred ).map( NamedSqlTypeNames::javaType ),
				SqlTypes.INTEGER, Optional.ofNullable( converter ).map( NamedSqlTypeNames::javaType ), Optional.empty(), Optional.empty(), ArrayNamingRepresentation.VARRAY );
	}

	private static void inspect(Dialect dialect, ImplicitNamingStrategy naming, Class<?> entity, Consumer<Metadata> assertions) {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", dialect )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.boot.metadata_serialization.enabled", true )
				.applySetting( "hibernate.type.prefer_native_enum_types", true ).build() ) {
			assertions.accept( MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( entity ), naming, new TypeNaming( true ) ) );
		}
	}

	private static String columnType(Metadata metadata, Class<?> entity, String property) {
		return ((BasicValue) metadata.getEntityBinding( entity.getName() ).getProperty( property ).getValue())
				.getColumns().get( 0 ).getSqlType( metadata );
	}

	static class Naming extends StandardImplicitNamingStrategy {
		final List<EnumNamingInput> enums = new ArrayList<>();
		final List<ArrayNamingInput> arrays = new ArrayList<>();
		final List<String> events = new ArrayList<>();
		boolean forbidden;

		@Override @Nonnull
		public LogicalName determineEnumName(@Nonnull EnumNamingInput input, @Nonnull ImplicitNamingContext context) {
			if ( forbidden ) { throw new AssertionError( "Implicit naming replayed" ); }
			enums.add( input );
			events.add( "enum:" + input.javaType().simpleName() );
			return context.implicitName( "e_" + input.javaType().simpleName() );
		}

		@Override @Nonnull
		public LogicalName determineArrayName(@Nonnull ArrayNamingInput input, @Nonnull ImplicitNamingContext context) {
			if ( forbidden ) { throw new AssertionError( "Implicit naming replayed" ); }
			arrays.add( input );
			events.add( "array:" + input.elementJavaType().simpleName() );
			return context.implicitName( "a_" + input.namedElement().map( n -> n.name().logicalName().getText() ).orElse( input.elementJavaType().simpleName() ) );
		}
	}
}
