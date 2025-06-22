/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.secondarytable;

import java.time.Instant;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {Record.class,SpecialRecord.class})
@SessionFactory
public class SecondaryRowTest {
	@Test
	void testSecondaryTableOptionality(SessionFactoryScope scope) {
		scope.inSession( (session) -> {
			verifySecondaryRows( "Optional", 0, session );
			verifySecondaryRows( "NonOptional", 0, session );
		} );

		final Record created = scope.fromTransaction( (session) -> {
			Record record = new Record();
			record.enabled = true;
			record.text = "Hello World!";

			session.persist( record );
			return record;
		} );
		scope.inSession( (session) -> {
			verifySecondaryRows( "Optional", 0, session );
			verifySecondaryRows( "NonOptional", 1, session );
		} );

		created.comment = "I was here";
		final Record merged = scope.fromTransaction( (session) -> session.merge( created ) );
		scope.inSession( (session) -> {
			verifySecondaryRows( "Optional", 1, session );
			verifySecondaryRows( "NonOptional", 1, session );
		} );

		merged.comment = null;
		scope.inTransaction( (session) -> session.merge( merged ) );
		scope.inSession( (session) -> {
			verifySecondaryRows( "Optional", 0, session );
			verifySecondaryRows( "NonOptional", 1, session );
		} );
	}

	@Test
	public void testOwnedSecondaryTable(SessionFactoryScope scope) {
		final String View_name = scope.getSessionFactory().getJdbcServices().getDialect().quote( "`View`" );
		verifySecondaryRows( View_name, 0, scope );

		final SpecialRecord created = scope.fromTransaction( (session) -> {
			final SpecialRecord record = new SpecialRecord();
			record.enabled = true;
			record.text = "Hello World!";
			session.persist( record );
			return record;
		} );
		verifySecondaryRows( View_name, 0, scope );

		created.timestamp = Instant.now();
		final SpecialRecord merged = scope.fromTransaction( (session) -> session.merge( created ) );
		verifySecondaryRows( View_name, 0, scope );
	}

	@AfterEach
	void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private static void verifySecondaryRows(String table, int expectedCount, SessionFactoryScope sfScope) {
		sfScope.inTransaction( (session) -> verifySecondaryRows( table, expectedCount, session ) );
	}

	private static void verifySecondaryRows(String table, int expectedCount, SessionImplementor session) {
		final String sql = "select count(1) from " + table;
		final int count = (int) session.createNativeQuery( sql, Integer.class ).getSingleResult();
		assertThat( count ).isEqualTo( expectedCount );
	}
}
