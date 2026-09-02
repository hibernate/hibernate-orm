/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the type and LOB contracts exposed by the standalone provider fixture.
///
/// @author Steve Ebersole
public class ExampleTypeAndLobSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesStableEnumLifecycleAndRelationalChecks() {
		final var support = dialect.getEnumSupport();
		assertSame( support, dialect.getEnumSupport() );
		assertEquals( "fixture_enum(priority:low|high)",
				support.getTypeDeclaration( "priority", new String[] { "low", "high" } ) );
		assertEquals(
				List.of( "create fixture enum priority values low|high" ),
				List.of( support.getCreateTypeCommands( "priority", new String[] { "low", "high" } ) )
		);
		assertEquals( List.of( "drop fixture enum priority" ), List.of( support.getDropTypeCommands( "priority" ) ) );
		assertEquals(
				"fixture(priority in ('low','high') or priority is null)",
				support.getCheckCondition(
						"priority",
						Arrays.asList( "low", "high", null ),
						VarcharJdbcType.INSTANCE
				)
		);
	}

	@Test
	void suppliesTypedNullRowIdAndObjectNullPolicies() {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final JdbcMapping jdbcMapping = typeConfiguration.getBasicTypeForJavaType( String.class );
		final SqlTypedMapping mapping = new SqlTypedMapping() {
			@Override public Long getLength() { return 64L; }
			@Override public Integer getArrayLength() { return null; }
			@Override public Integer getPrecision() { return null; }
			@Override public Integer getScale() { return null; }
			@Override public Integer getTemporalPrecision() { return null; }
			@Override public JdbcMapping getJdbcMapping() { return jdbcMapping; }
		};
		assertEquals( "cast(null as fixture_null_12)",
				dialect.getSelectClauseNullString( mapping, typeConfiguration ) );

		final var rowIds = dialect.getRowIdSupport();
		assertSame( rowIds, dialect.getRowIdSupport() );
		assertEquals( "fixture_rowid", rowIds.resolveExpression( null ) );
		assertEquals( "custom_rowid fixture rowid generated always", rowIds.columnDefinition( "custom_rowid" ) );
		assertEquals( Types.ROWID, rowIds.sqlTypeCode() );
		assertEquals( ObjectNullBindingStrategy.SET_OBJECT, dialect.getObjectNullBindingStrategy() );
	}

	@Test
	void composesLobAndStringPoliciesWithoutInternalDependencies() {
		final var lobs = dialect.getLobSupport();
		assertSame( lobs, dialect.getLobSupport() );
		assertFalse( lobs.supportsJdbcConnectionLobCreation( null ) );
		assertFalse( lobs.useInputStreamToInsertBlob() );
		assertNull( lobs.getValueLobFragmentForExtraCreateTableInfo( "plain_lob" ) );
		assertEquals(
				" fixture value lob(payload)",
				lobs.getValueLobFragmentForExtraCreateTableInfo( "payload" )
		);

		assertSame(
				StringValueSemantics.EMPTY_STRING_AS_NULL_AND_CHAR_TRAILING_SPACES_STRIPPED,
				dialect.getStringValueSemantics()
		);
		assertTrue( dialect.getStringValueSemantics().treatsEmptyStringAsNull() );
		assertTrue( dialect.getStringValueSemantics().stripsCharTrailingSpaces() );
		assertTrue( dialect.fixtureStringToBooleanCast( "1", "0" ).contains( "('true',1)" ) );
		assertTrue( dialect.fixtureStringToBooleanCastDecode( "1", "0" ).contains( "decode(lower(?1)" ) );
	}
}
