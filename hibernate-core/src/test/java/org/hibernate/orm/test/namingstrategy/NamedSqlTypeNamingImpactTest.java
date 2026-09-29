/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.type.internal.OracleArrayJdbcTypeConstructor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.BasicValue;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/// Verifies that physical SQL type names reach both export and runtime consumers.
/// Uses metadata and mocked JDBC boundaries without a database connection.
///
/// @author Steve Ebersole
@BaseUnitTest
class NamedSqlTypeNamingImpactTest {
	@Test void postgresEnumDefault() { enumNames( new PostgreSQLDialect(), false ); }
	@Test void postgresEnumPhysicalOverride() { enumNames( new PostgreSQLDialect(), true ); }
	@Test void oracleEnumDefault() { enumNames( new OracleTypesDialect(), false ); }
	@Test void oracleEnumPhysicalOverride() { enumNames( new OracleTypesDialect(), true ); }
	@Test void oracleArrayDefault() throws Exception { arrayNames( false ); }
	@Test void oracleArrayPhysicalOverride() throws Exception { arrayNames( true ); }


	@Test
	void dispatchesByNamedObjectKind() {
		final var enumNaming = new TypeNaming( true );
		inspect( new PostgreSQLDialect(), enumNaming, EnumArrayEntity.class, metadata ->
				assertThat( enumNaming.kinds ).containsExactly( "enum" ) );
		final var arrayNaming = new TypeNaming( true );
		inspect( new OracleTypesDialect(), arrayNaming, ArrayEntity.class, metadata ->
				assertThat( arrayNaming.kinds ).containsExactly( "array" ) );
		final var structNaming = new TypeNaming( true );
		inspect( new OracleTypesDialect(), structNaming, StructArrayEntity.class, metadata ->
				assertThat( structNaming.kinds ).containsExactlyInAnyOrder( "struct", "array" ) );
	}

	@Test
	void cachesEachKindIndependently() {
		final var naming = new TypeNaming( true );
		inspect( new OracleTypesDialect(), naming, ArrayEntity.class, metadata -> {
			final var namespace = metadata.getDatabase().getDefaultNamespace();
			final var name = org.hibernate.boot.model.naming.Identifier.toIdentifier( "Shared" );
			naming.kinds.clear();
			for ( int i = 0; i < 2; i++ ) {
				namespace.resolvePhysicalEnumName( name );
				namespace.resolvePhysicalArrayName( name );
				namespace.resolvePhysicalStructName( name );
			}
			assertThat( naming.kinds ).containsExactly( "enum", "array", "struct" );
		} );
	}

	@Test
	void ordinalEnumsUsePhysicalNames() {
		for ( var dialect : List.of( new PostgreSQLDialect(), new OracleTypesDialect() ) ) {
			inspect( dialect, new TypeNaming( true ), OrdinalEntity.class, metadata -> {
				final var type = value( metadata, OrdinalEntity.class, "state" ).getType();
				final var ddl = metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry();
				assertThat( ddl.getTypeName( SqlTypes.NAMED_ORDINAL_ENUM, Size.nil(), type ) ).isEqualTo( "p_State" );
				assertThat( createSql( metadata ) ).contains( "p_State" );
			} );
		}
	}

	@Test
	void convertedEnumValuesKeepPhysicalTypeName() {
		for ( var dialect : List.of( new PostgreSQLDialect(), new OracleTypesDialect() ) ) {
			inspect( dialect, new TypeNaming( true ), ConvertedEnumEntity.class, metadata -> {
				assertThat( value( metadata, ConvertedEnumEntity.class, "state" ).getColumns().get( 0 ).getSqlType( metadata ) )
						.isEqualTo( "p_Code" );
				assertThat( createSql( metadata ) ).contains( "p_Code", "'A'", "'I'" );
			} );
		}
	}

	@Test
	void enumArrayUsesResolvedElementName() {
		final var naming = new TypeNaming( true );
		inspect( new PostgreSQLDialect(), naming, EnumArrayEntity.class, metadata -> {
			assertThat( value( metadata, EnumArrayEntity.class, "states" ).getColumns().get( 0 ).getSqlType( metadata ) )
					.isEqualTo( "p_State array" );
			assertThat( createSql( metadata ) ).contains( "p_State" );
			assertThat( naming.calls ).containsExactly( "State" );
		} );
	}

	@Test
	void repeatedEnumReferencesResolveOnce() {
		final var naming = new TypeNaming( true );
		inspect( new PostgreSQLDialect(), naming, RepeatedEnumEntity.class, metadata -> {
			assertThat( value( metadata, RepeatedEnumEntity.class, "first" ).getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "p_State" );
			assertThat( value( metadata, RepeatedEnumEntity.class, "second" ).getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "p_State" );
			assertThat( naming.calls ).containsExactly( "State" );
			assertThat( metadata.getDatabase().getAuxiliaryDatabaseObjects() ).hasSize( 1 );
		} );
	}

	@Test
	void quotedArrayNamesIncludeHelperSuffixInsideQuotes() {
		final var naming = new TypeNaming( true, true );
		inspect( new OracleTypesDialect(), naming, ArrayEntity.class, metadata -> {
			final var value = value( metadata, ArrayEntity.class, "numbers" );
			assertThat( value.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "\"p_IntegerArray\"" );
			final var udt = metadata.getDatabase().getDefaultNamespace().getUserDefinedTypes().iterator().next();
			final var context = SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final var exporter = metadata.getDatabase().getDialect().getUserDefinedTypeExporter();
			assertThat( String.join( " ", exporter.getSqlCreateStrings( udt, metadata, context ) ) )
					.contains( "\"p_IntegerArray_position\"(" ).doesNotContain( "\"p_IntegerArray\"_" );
			assertThat( String.join( " ", exporter.getSqlDropStrings( udt, metadata, context ) ) )
					.contains( "\"p_IntegerArray_position\"" ).doesNotContain( "\"p_IntegerArray\"_" );
			final var statement = mock( java.sql.CallableStatement.class );
			try {
				value.resolve().getJdbcType().registerOutParameter( statement, 1 );
				verify( statement ).registerOutParameter( 1, Types.ARRAY, "\"p_IntegerArray\"" );
			}
			catch (java.sql.SQLException e) { throw new AssertionError( e ); }
		} );
	}

	@Test
	void restoredEnumAndArrayNamesDoNotInvokeStrategy() {
		for ( var entity : List.of( EnumEntity.class, ArrayEntity.class ) ) {
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( "hibernate.dialect", new OracleTypesDialect() )
					.applySetting( "hibernate.boot.metadata_serialization.enabled", true )
				.applySetting( "hibernate.type.prefer_native_enum_types", true )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false ).build() ) {
				final var naming = new TypeNaming( true, true );
				final var metadata = MetadataBuildingTestHelper.buildMetadataWithPhysicalNaming(
						registry, new MappingSources().addManagedClass( entity ), naming );
				final var bytes = new java.io.ByteArrayOutputStream();
				org.hibernate.boot.serial.MetadataSerialization.serialize( (org.hibernate.boot.spi.MetadataImplementor) metadata ).writeTo( bytes );
				naming.forbidNaming = true;
				final var restored = org.hibernate.boot.serial.MetadataSerialization.read(
						new java.io.ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
				final var property = entity == EnumEntity.class ? "state" : "numbers";
				assertThat( value( restored, entity, property ).getColumns().get( 0 ).getSqlType( restored ) )
						.isEqualTo( value( metadata, entity, property ).getColumns().get( 0 ).getSqlType( metadata ) );
				assertThat( ((SqlTypedJdbcType) value( restored, entity, property ).resolve().getJdbcType()).getSqlTypeName() )
						.isEqualTo( entity == EnumEntity.class ? "\"p_State\"" : "\"p_IntegerArray\"" );
			}
		}
	}

	@Test
	void qualifiedNamesAgreeWithExportAndJdbc() throws Exception {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", new OracleTypesDialect() )
				.applySetting( "hibernate.default_schema", "TypeSchema" )
				.applySetting( "hibernate.boot.metadata_serialization.enabled", true )
				.applySetting( "hibernate.type.prefer_native_enum_types", true )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false ).build() ) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithPhysicalNaming(
					registry, new MappingSources().addManagedClass( ArrayEntity.class ).addManagedClass( EnumEntity.class ),
					new TypeNaming( true, true ) );
			final var array = value( metadata, ArrayEntity.class, "numbers" );
			assertThat( array.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "TypeSchema.\"p_IntegerArray\"" );
			assertThat( createSql( metadata ) ).contains( "TypeSchema.\"p_State\"" );
			final var statement = mock( java.sql.CallableStatement.class );
			array.resolve().getJdbcType().registerOutParameter( statement, "result" );
			verify( statement ).registerOutParameter( "result", Types.ARRAY, "TYPESCHEMA.\"p_IntegerArray\"" );
		}
	}

	@Test
	void structArrayReusesItsFinalizedName() {
		final var naming = new TypeNaming( true );
		inspect( new OracleTypesDialect(), naming, StructArrayEntity.class, metadata -> {
			final var column = metadata.getEntityBinding( StructArrayEntity.class.getName() ).getTable()
					.getColumns().stream().filter( c -> !c.getName().equals( "id" ) ).findFirst().orElseThrow();
			final var types = metadata.getDatabase().getDefaultNamespace().getDependencyOrderedUserDefinedTypes();
			assertThat( types ).hasSize( 2 );
			final var array = types.stream().filter( t -> t instanceof org.hibernate.mapping.UserDefinedArrayType ).findFirst().orElseThrow();
			assertThat( column.getSqlType( metadata ) ).isEqualTo( array.getName() );
			assertThat( array.getName() ).startsWith( "p_" ).doesNotContain( "p_p_" );
		} );
	}


	@Test
	void nestedTableUsesPhysicalName() {
		inspect( new OracleTypesDialect(), new TypeNaming( true ), NestedTableEntity.class, metadata -> {
			final var type = value( metadata, NestedTableEntity.class, "numbers" );
			assertThat( type.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "p_IntegerArray" );
			assertThat( type.resolve().getJdbcType().getDdlTypeCode() ).isEqualTo( SqlTypes.TABLE );
			final var array = (org.hibernate.mapping.UserDefinedArrayType) metadata.getDatabase().getDefaultNamespace()
					.getUserDefinedTypes().iterator().next();
			assertThat( array.getName() ).isEqualTo( "p_IntegerArray" );
			assertThat( array.getArraySqlTypeCode() ).isEqualTo( SqlTypes.TABLE );
		} );
	}

	@Test
	void extractedArrayTypeNamesAreAlreadyPhysical() {
		final var info = mock( org.hibernate.tool.schema.extract.spi.ColumnTypeInformation.class );
		org.mockito.Mockito.when( info.getTypeName() ).thenReturn( "\"ExistingArray\"" );
		final var descriptor = new OracleArrayJdbcTypeConstructor().resolveType(
				new org.hibernate.type.spi.TypeConfiguration(), new OracleTypesDialect(),
				org.hibernate.type.descriptor.jdbc.IntegerJdbcType.INSTANCE, info );
		assertThat( ((SqlTypedJdbcType) descriptor).getSqlTypeName() ).isEqualTo( "\"ExistingArray\"" );
	}


	@Test
	void explicitJavaTypeAndAttributeConverterResolvePhysicalEnumNames() {
		for ( var dialect : List.of( new PostgreSQLDialect(), new OracleTypesDialect() ) ) {
			for ( var entity : List.of( ExplicitJavaTypeEntity.class, AttributeConvertedEntity.class ) ) {
				inspect( dialect, new TypeNaming( true ), entity, metadata -> {
					assertThat( value( metadata, entity, "state" ).getColumns().get( 0 ).getSqlType( metadata ) )
							.isEqualTo( "p_State" );
					assertThat( createSql( metadata ) ).contains( "p_State" );
				} );
			}
		}
	}


	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	void enumArrayJdbcCreationUsesPhysicalElementName() {
		inspect( new PostgreSQLDialect(), new TypeNaming( true, true ), EnumArrayEntity.class, metadata -> {
			final var resolution = value( metadata, EnumArrayEntity.class, "states" ).resolve();
			final var options = mock( WrapperOptions.class, org.mockito.Mockito.RETURNS_DEEP_STUBS );
			org.mockito.Mockito.when( options.getTypeConfiguration() ).thenReturn( metadata.getDatabase().getTypeConfiguration() );
			org.mockito.Mockito.when( options.getDialect() ).thenReturn( metadata.getDatabase().getDialect() );
			final var connection = options.getSession().getJdbcCoordinator().getLogicalConnection().getPhysicalConnection();
			try {
				((org.hibernate.type.descriptor.ValueBinder) resolution.getLegacyResolvedBasicType().getJdbcValueBinder())
						.bind( mock( PreparedStatement.class ), new State[] { State.ACTIVE }, 1, options );
				verify( connection ).createArrayOf( org.mockito.ArgumentMatchers.eq( "\"p_State\"" ),
						org.mockito.ArgumentMatchers.any( Object[].class ) );
			}
			catch (java.sql.SQLException e) { throw new AssertionError( e ); }
		} );
	}


	@Test
	void oracleEnumArrayKeepsBothPhysicalNames() {
		final var naming = new TypeNaming( true );
		inspect( new OracleTypesDialect(), naming, EnumArrayEntity.class, metadata -> {
			final var value = value( metadata, EnumArrayEntity.class, "states" );
			assertThat( value.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( "p_StateArray" );
			assertThat( createSql( metadata ) ).contains( "p_State" );
			final var array = (org.hibernate.mapping.UserDefinedArrayType) metadata.getDatabase().getDefaultNamespace()
					.getUserDefinedTypes().iterator().next();
			assertThat( array.getName() ).isEqualTo( "p_StateArray" );
			assertThat( array.getElementTypeName() ).isEqualTo( "p_State" );
			assertThat( naming.calls ).containsExactly( "State", "StateArray" );
		} );
	}


	private static BasicValue value(Metadata metadata, Class<?> entity, String property) {
		return (BasicValue) metadata.getEntityBinding( entity.getName() ).getProperty( property ).getValue();
	}

	private static String createSql(Metadata metadata) {
		final var context = SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
		return metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
				.map( object -> String.join( " ", object.sqlCreateStrings( context ) ) )
				.collect( java.util.stream.Collectors.joining( " " ) );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void enumNames(Dialect dialect, boolean renamed) {
		final var naming = new TypeNaming( renamed );
		final var expected = renamed ? "p_State" : "State";
		inspect( dialect, naming, EnumEntity.class, metadata -> {
			final var value = (BasicValue) metadata.getEntityBinding( EnumEntity.class.getName() ).getProperty( "state" ).getValue();
			final var resolution = value.resolve();
			final var type = resolution.getLegacyResolvedBasicType();
			final var ddl = metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry().getDescriptor( SqlTypes.NAMED_ENUM );
			assertThat( value.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( expected );
			assertThat( ddl.getCastTypeName( Size.nil(), type, metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry() ) ).isEqualTo( expected );
			final var formatter = resolution.getJdbcType().getJdbcLiteralFormatter( resolution.getRelationalJavaType() );
			assertThat( ((org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter) formatter)
					.toJdbcLiteral( State.ACTIVE, dialect, mock( WrapperOptions.class ) ) ).contains( expected );
			final var context = SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final var objects = metadata.getDatabase().getAuxiliaryDatabaseObjects();
			assertThat( objects ).hasSize( 1 );
			final var object = objects.iterator().next();
			assertThat( String.join( " ", object.sqlCreateStrings( context ) ) ).contains( expected );
			assertThat( String.join( " ", object.sqlDropStrings( context ) ) ).contains( expected );
			assertThat( naming.calls ).containsExactly( "State" );
		} );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void arrayNames(boolean renamed) throws Exception {
		final var naming = new TypeNaming( renamed );
		final var statement = mock( PreparedStatement.class );
		final var expected = renamed ? "p_IntegerArray" : "IntegerArray";
		inspect( new OracleTypesDialect(), naming, ArrayEntity.class, metadata -> {
			final var value = (BasicValue) metadata.getEntityBinding( ArrayEntity.class.getName() ).getProperty( "numbers" ).getValue();
			final var resolution = value.resolve();
			final var jdbcType = resolution.getJdbcType();
			assertThat( jdbcType ).isInstanceOf( SqlTypedJdbcType.class );
			assertThat( ((SqlTypedJdbcType) jdbcType).getSqlTypeName() ).isEqualTo( expected );
			assertThat( value.getColumns().get( 0 ).getSqlType( metadata ) ).isEqualTo( expected );
			final var ddlTypes = metadata.getDatabase().getTypeConfiguration().getDdlTypeRegistry();
			assertThat( ddlTypes.getDescriptor( SqlTypes.ARRAY ).getCastTypeName( Size.nil(), resolution.getLegacyResolvedBasicType(), ddlTypes ) )
					.isEqualTo( expected );
			final var namespace = metadata.getDatabase().getDefaultNamespace();
			assertThat( namespace.getUserDefinedTypes() ).hasSize( 1 );
			final var udt = namespace.getUserDefinedTypes().iterator().next();
			assertThat( udt.getName() ).isEqualTo( expected );
			final var context = SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final var exporter = metadata.getDatabase().getDialect().getUserDefinedTypeExporter();
			assertThat( String.join( " ", exporter.getSqlCreateStrings( udt, metadata, context ) ) ).contains( expected );
			assertThat( String.join( " ", exporter.getSqlDropStrings( udt, metadata, context ) ) ).contains( expected );
			assertThat( naming.calls ).containsExactly( "IntegerArray" );
			try {
				((org.hibernate.type.descriptor.ValueBinder) jdbcType.getBinder( resolution.getRelationalJavaType() ))
						.bind( statement, null, 1, mock( WrapperOptions.class ) );
			}
			catch (java.sql.SQLException e) { throw new AssertionError( e ); }
		} );
		verify( statement ).setNull( 1, Types.ARRAY, renamed ? "P_INTEGERARRAY" : "INTEGERARRAY" );
	}

	private void inspect(Dialect dialect, TypeNaming naming, Class<?> entity, Consumer<Metadata> assertion) {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", dialect )
				.applySetting( "hibernate.boot.metadata_serialization.enabled", true )
				.applySetting( "hibernate.type.prefer_native_enum_types", true )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false ).build()) {
			assertion.accept( MetadataBuildingTestHelper.buildMetadataWithPhysicalNaming(
					registry, new MappingSources().addManagedClass( entity ), naming ) );
		}
	}

	/// Registers the real array descriptor without requiring driver discovery or a connection.
	static class OracleTypesDialect extends OracleDialect {
		OracleTypesDialect() { super( DatabaseVersion.make( 23 ) ); }
		@Override
		public void contributeTypes(TypeContributions contributions, ServiceRegistry registry) {
			super.contributeTypes( contributions, registry );
			contributions.contributeJdbcTypeConstructor( new OracleArrayJdbcTypeConstructor() );
			contributions.contributeJdbcTypeConstructor( new org.hibernate.dialect.type.internal.OracleNestedTableJdbcTypeConstructor() );
		}
	}

	static class TypeNaming extends PhysicalNamingStrategyStandardImpl {
		final boolean renamed;
		final boolean quoted;
		boolean forbidNaming;
		final List<String> calls = new ArrayList<>();
		final List<String> kinds = new ArrayList<>();
		TypeNaming(boolean renamed) { this( renamed, false ); }
		TypeNaming(boolean renamed, boolean quoted) { this.renamed = renamed; this.quoted = quoted; }
		@Override @Nonnull
		public PhysicalName toPhysicalEnumName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			if ( forbidNaming ) { throw new AssertionError( "Naming replayed during restoration" ); }
			calls.add( name.getText() );
			kinds.add( "enum" );
			return context.getPhysicalNameFactory().create( (renamed ? "p_" : "") + name.getText(), quoted || name.isQuoted() );
		}

		@Override @Nonnull
		public PhysicalName toPhysicalArrayName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			if ( forbidNaming ) { throw new AssertionError( "Naming replayed during restoration" ); }
			calls.add( name.getText() );
			kinds.add( "array" );
			return context.getPhysicalNameFactory().create( (renamed ? "p_" : "") + name.getText(), quoted || name.isQuoted() );
		}

		@Override @Nonnull
		public PhysicalName toPhysicalStructName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			if ( forbidNaming ) { throw new AssertionError( "Naming replayed during restoration" ); }
			calls.add( name.getText() );
			kinds.add( "struct" );
			return context.getPhysicalNameFactory().create( (renamed ? "p_" : "") + name.getText(), quoted || name.isQuoted() );
		}
	}

	@Entity static class OrdinalEntity { @Id long id; @JdbcTypeCode(SqlTypes.NAMED_ORDINAL_ENUM) State state; }
	@Entity static class ConvertedEnumEntity { @Id long id; @JdbcTypeCode(SqlTypes.NAMED_ENUM) @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING) Code state; }
	enum Code {
		ACTIVE("A"), INACTIVE("I");
		@jakarta.persistence.EnumeratedValue final String code;
		Code(String code) { this.code = code; }
	}
	@Entity static class EnumArrayEntity { @Id long id; @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING) State[] states; }
	@Entity static class RepeatedEnumEntity {
		@Id long id;
		@JdbcTypeCode(SqlTypes.NAMED_ENUM) State first;
		@JdbcTypeCode(SqlTypes.NAMED_ENUM) State second;
	}

	@jakarta.persistence.Embeddable static class Piece { String text; }
	@Entity static class StructArrayEntity { @Id long id; @org.hibernate.annotations.Struct(name = "Piece") Piece[] pieces; }

	@Entity static class NestedTableEntity { @Id long id; @JdbcTypeCode(SqlTypes.TABLE) Integer[] numbers; }

	public static class StateJavaType extends org.hibernate.type.descriptor.java.EnumJavaType<State> {
		public StateJavaType() { super( State.class ); }
	}
	@Entity static class ExplicitJavaTypeEntity {
		@Id long id;
		@org.hibernate.annotations.JavaType(StateJavaType.class)
		@JdbcTypeCode(SqlTypes.NAMED_ENUM) State state;
	}
	@jakarta.persistence.Converter
	public static class StateConverter implements jakarta.persistence.AttributeConverter<State, String> {
		@Override public String convertToDatabaseColumn(State value) { return value == null ? null : value.name(); }
		@Override public State convertToEntityAttribute(String value) { return value == null ? null : State.valueOf( value ); }
	}
	@Entity static class AttributeConvertedEntity {
		@Id long id;
		@jakarta.persistence.Convert(converter = StateConverter.class)
		@JdbcTypeCode(SqlTypes.NAMED_ENUM) State state;
	}

	enum State { ACTIVE, INACTIVE }
	@Entity static class EnumEntity { @Id long id; @JdbcTypeCode(SqlTypes.NAMED_ENUM) State state; }
	@Entity static class ArrayEntity { @Id long id; Integer[] numbers; }
}
