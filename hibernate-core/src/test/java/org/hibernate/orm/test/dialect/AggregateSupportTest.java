/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Nullable;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.aggregate.spi.AggregateArrayElementDescriptor;
import org.hibernate.dialect.aggregate.spi.AggregateAuxiliaryObjectRequest;
import org.hibernate.dialect.aggregate.spi.AggregateColumnDescriptor;
import org.hibernate.dialect.aggregate.spi.AggregateCustomWriteRequest;
import org.hibernate.dialect.aggregate.spi.AggregateSqlAuxiliaryObject;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.spi.AggregateSupports;
import org.hibernate.dialect.aggregate.spi.AggregateWriteRendererRequest;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the supported aggregate mapping and rendering contracts.
///
/// @author Steve Ebersole
/// @since 8.0
public class AggregateSupportTest {
	private static final TypeConfiguration TYPES = new TypeConfiguration();
	private static final SqlTypedMapping STRING_MAPPING = new SqlTypedMappingImpl(
			255L,
			null,
			null,
			null,
			null,
			new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE )
	);

	@Test
	void standardProfileIsStableAndPreservesDefaults() {
		final AggregateSupport standard = AggregateSupports.standard();
		assertSame( standard, AggregateSupports.standard() );
		assertTrue( standard.preferSelectAggregateMapping( SqlTypes.JSON ) );
		assertTrue( standard.preferBindAggregateMapping( SqlTypes.STRUCT ) );
		assertTrue( standard.supportsComponentCheckConstraints() );
		assertEquals( SqlTypes.JSON_ARRAY, standard.aggregateComponentSqlTypeCode( SqlTypes.JSON, SqlTypes.ARRAY ) );
		assertEquals( SqlTypes.XML_ARRAY, standard.aggregateComponentSqlTypeCode( SqlTypes.SQLXML, SqlTypes.ARRAY ) );
	}

	@Test
	void requestsDefensivelyCopyOrderedComponents() {
		final var scalar = descriptor( "street", SqlTypes.VARCHAR, List.of(), null );
		final var components = new ArrayList<AggregateColumnDescriptor>();
		components.add( scalar );
		final var aggregate = descriptor( "address", SqlTypes.STRUCT, components, null );

		final var writeRequest = new AggregateCustomWriteRequest( aggregate, components, TYPES );
		final var auxiliaryRequest = new AggregateAuxiliaryObjectRequest(
				"address",
				aggregate,
				components,
				TYPES,
				false
		);
		components.clear();

		assertEquals( List.of( scalar ), writeRequest.components() );
		assertEquals( List.of( scalar ), auxiliaryRequest.components() );
		assertThrows( UnsupportedOperationException.class, () -> writeRequest.components().clear() );
		assertThrows( NullPointerException.class, () -> new AggregateCustomWriteRequest( aggregate, null, TYPES ) );
		assertThrows( NullPointerException.class, () -> new AggregateWriteRendererRequest( null, List.of(), TYPES ) );
	}

	@Test
	void descriptorsRepresentNestedComponentsAndStructuredArrays() {
		final var street = descriptor( "street", SqlTypes.VARCHAR, List.of(), null );
		final var address = descriptor( "address", SqlTypes.STRUCT, List.of( street ), null );
		final var element = new AggregateArrayElementDescriptor( "address_t", SqlTypes.STRUCT, SqlTypes.STRUCT, 42 );
		final var addresses = descriptor( "addresses", SqlTypes.STRUCT_ARRAY, List.of( address ), element );

		assertEquals( street, addresses.components().get( 0 ).components().get( 0 ) );
		assertSame( element, addresses.arrayElement() );
		assertEquals( 42, addresses.arrayElement().arrayLength() );
	}

	@Test
	void sqlAuxiliaryResultsAreValidatedAndImmutable() {
		final var creates = new ArrayList<>( List.of( "create fixture" ) );
		final var object = new AggregateSqlAuxiliaryObject(
				"fixture",
				creates,
				List.of( "drop fixture" ),
				Set.of( DB2Dialect.class.getName() ),
				false
		);
		creates.clear();
		assertEquals( List.of( "create fixture" ), object.createCommands() );
		assertThrows( UnsupportedOperationException.class, () -> object.createCommands().clear() );
		assertThrows(
				IllegalArgumentException.class,
				() -> new AggregateSqlAuxiliaryObject( " ", List.of( "create" ), List.of(), Set.of(), false )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new AggregateSqlAuxiliaryObject( "empty", List.of(), List.of(), Set.of(), false )
		);
	}

	@Test
	void maintainedDialectsSupplyInternalImplementations() {
		for ( var support : List.of(
				new H2Dialect( DatabaseVersion.make( 2, 2, 220 ) ).getAggregateSupport(),
				new HANADialect().getAggregateSupport(),
				new DB2Dialect().getAggregateSupport(),
				new CockroachDialect().getAggregateSupport(),
				new MySQLDialect().getAggregateSupport(),
				new MariaDBDialect().getAggregateSupport(),
				new OracleDialect().getAggregateSupport(),
				new PostgreSQLDialect().getAggregateSupport(),
				new SpannerPostgreSQLDialect().getAggregateSupport(),
				new SQLServerDialect().getAggregateSupport(),
				new SybaseASEDialect().getAggregateSupport() ) ) {
			assertNotNull( support );
			assertTrue( support.getClass().getPackageName().endsWith( ".aggregate.internal" ) );
		}
	}

	@Test
	void supportedSignaturesDoNotExposeBootMappingImplementations() {
		final Set<Class<?>> forbidden = Set.of(
				AggregateColumn.class,
				Column.class,
				Namespace.class,
				AuxiliaryDatabaseObject.class
		);
		for ( var method : AggregateSupport.class.getDeclaredMethods() ) {
			assertFalse( forbidden.contains( method.getReturnType() ), method::toString );
			for ( var parameterType : method.getParameterTypes() ) {
				assertFalse( forbidden.contains( parameterType ), method::toString );
			}
		}
		for ( Class<?> requestType : List.of(
				AggregateCustomWriteRequest.class,
				AggregateAuxiliaryObjectRequest.class,
				AggregateWriteRendererRequest.class ) ) {
			for ( RecordComponent component : requestType.getRecordComponents() ) {
				assertFalse( forbidden.contains( component.getType() ), component::toString );
			}
		}
	}

	private static AggregateColumnDescriptor descriptor(
			String name,
			int typeCode,
			List<AggregateColumnDescriptor> components,
			@Nullable AggregateArrayElementDescriptor arrayElement) {
		return new AggregateColumnDescriptor() {
			@Override
			public String columnName() {
				return name;
			}

			@Override
			public int sqlTypeCode() {
				return typeCode;
			}

			@Override
			public String sqlTypeName() {
				return name + "_type";
			}

			@Override
			public SqlTypedMapping typeMapping() {
				return STRING_MAPPING;
			}

			@Override
			public boolean nullable() {
				return true;
			}

			@Override
			public List<AggregateColumnDescriptor> components() {
				return List.copyOf( components );
			}

			@Override
			public @Nullable AggregateArrayElementDescriptor arrayElement() {
				return arrayElement;
			}
		};
	}
}
