/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.internal.QualifiedObjectNameFormatterStandardImpl;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.persister.entity.ExplicitSqlStringGenerationContext;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Physical SQL names retain finalized quoting, with JDBC qualifier ordering and defaults.
///
/// @author Steve Ebersole
class QualifiedPhysicalNameRenderingTest {
	private final H2Dialect dialect = new H2Dialect();
	private final PhysicalName.Factory factory = new PhysicalName.Factory( (text, quoted) -> text );

	@Test
	void honorsQualifierSupportAndCatalogPosition() {
		final var name = new QualifiedPhysicalName( factory.create( "cat", true ),
				factory.create( "Schema", true ), factory.create( "Seq", true ) );
		assertFormat( NameQualifierSupport.BOTH, false, name, "\"cat\":\"Schema\".\"Seq\"" );
		assertFormat( NameQualifierSupport.BOTH, true, name, "\"Schema\".\"Seq\":\"cat\"" );
		assertFormat( NameQualifierSupport.CATALOG, false, name, "\"cat\":\"Seq\"" );
		assertFormat( NameQualifierSupport.CATALOG, true, name, "\"Seq\":\"cat\"" );
		assertFormat( NameQualifierSupport.SCHEMA, false, name, "\"Schema\".\"Seq\"" );
		assertFormat( NameQualifierSupport.NONE, false, name, "\"Seq\"" );
		assertFormat( NameQualifierSupport.BOTH, false,
				new QualifiedPhysicalName( null, null, factory.create( "Mixed", false ) ), "Mixed" );
	}

	@Test
	void appliesDefaultsWithoutRenormalizingFinalizedNames() {
		final var environment = environment( true );
		final var context = SqlStringGenerationContextImpl.forTests( environment, "catalog", "schema" );
		final var name = new QualifiedPhysicalName( null, null, factory.create( "Unquoted", false ) );
		assertEquals( "\"catalog\".\"schema\".Unquoted", context.format( name ) );
		assertEquals( "\"schema\".Unquoted", context.formatWithoutCatalog( name ) );
		final var explicit = new QualifiedPhysicalName( factory.create( "own_catalog", false ),
				factory.create( "own_schema", false ), factory.create( "Quoted", true ) );
		assertEquals( "own_catalog.own_schema.\"Quoted\"", context.format( explicit ) );
		assertEquals( "own_schema.\"Quoted\"", context.formatWithoutCatalog( explicit ) );
	}

	@Test
	void honorsExplicitContextOverrides() {
		final var environment = environment( false );
		final var base = SqlStringGenerationContextImpl.forTests( environment, "base_catalog", "base_schema" );
		final var services = mock( JdbcServices.class );
		when( services.getJdbcEnvironment() ).thenReturn( environment );
		when( services.getDialect() ).thenReturn( dialect );
		final var context = new ExplicitSqlStringGenerationContext( "other_catalog", "other_schema", base, services );
		final var name = new QualifiedPhysicalName( null, null, factory.create( "Seq", true ) );
		assertEquals( "other_catalog.other_schema.\"Seq\"", context.format( name ) );
		assertEquals( "other_schema.\"Seq\"", context.formatWithoutCatalog( name ) );
	}

	private void assertFormat(NameQualifierSupport support, boolean catalogAtEnd, QualifiedPhysicalName name, String expected) {
		assertEquals( expected, new QualifiedObjectNameFormatterStandardImpl( support, ":", catalogAtEnd ).format( name, dialect ) );
	}

	private JdbcEnvironment environment(boolean globallyQuoted) {
		final var environment = mock( JdbcEnvironment.class );
		when( environment.getDialect() ).thenReturn( dialect );
		final var builder = IdentifierHelperBuilder.from( environment );
		builder.setGloballyQuoteIdentifiers( globallyQuoted );
		when( environment.getIdentifierHelper() ).thenReturn( builder.build() );
		when( environment.getQualifiedObjectNameFormatter() ).thenReturn(
				new QualifiedObjectNameFormatterStandardImpl( NameQualifierSupport.BOTH, ".", false ) );
		return environment;
	}
}
