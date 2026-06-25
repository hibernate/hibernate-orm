/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.Timestamp;
import java.util.Locale;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel( annotatedClasses = {
		AltibaseVersionSeedTest.VersionedEntry.class,
		AltibaseVersionSeedTest.SourceEntry.class
} )
@RequiresDialect( AltibaseDialect.class )
@SessionFactory( useCollectingStatementInspector = true )
public class AltibaseVersionSeedTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new SourceEntry( 1, "source" ) ) );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsertSelectTimestampVersionSeedUsesJdbcLiteralFormatter(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					final SourceEntry source = session.find( SourceEntry.class, 1 );
					assertThat( source.getId() ).isEqualTo( 1 );
					assertThat( source.getName() ).isEqualTo( "source" );
					statementInspector.clear();
					final int rows = session.createMutationQuery(
							"insert into VersionedEntry (id, name) " +
									"select 1, s.name from SourceEntry s"
					).executeUpdate();
					assertEquals( 1, rows );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					final String insertSql = statementInspector.getSqlQueries().get( 0 ).toLowerCase( Locale.ROOT );
					assertThat( insertSql ).contains( "{ts '" );
					assertThat( insertSql ).doesNotContain( "sysdate" );
					final VersionedEntry inserted = session.find( VersionedEntry.class, 1 );
					assertThat( inserted.getId() ).isEqualTo( 1 );
					assertThat( inserted.getName() ).isEqualTo( "source" );
					assertThat( inserted.getVersion() ).isNotNull();
				}
		);
	}

	@Test
	public void testInsertSelectDoesNotInlineNestedSubqueryParameters(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					final int rows = session.createMutationQuery(
									"insert into VersionedEntry (id, name) " +
											"select 2, (select :nestedName from SourceEntry s2 where s2.id = s.id) " +
											"from SourceEntry s"
							)
							.setParameter( "nestedName", "nested-name" )
							.executeUpdate();
					assertEquals( 1, rows );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
					final String insertSql = statementInspector.getSqlQueries().get( 0 ).toLowerCase( Locale.ROOT );
					assertThat( insertSql ).contains( "cast(?" );
					assertThat( insertSql ).doesNotContain( "nested-name" );
					final VersionedEntry inserted = session.find( VersionedEntry.class, 2 );
					assertThat( inserted.getName() ).isEqualTo( "nested-name" );
				}
		);
	}

	@Entity( name = "VersionedEntry" )
	@Table( name = "ALTIBASE_VERSIONED_ENTRY" )
	public static class VersionedEntry {
		@Id
		private Integer id;

		private String name;

		@Version
		private Timestamp version;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Timestamp getVersion() {
			return version;
		}
	}

	@Entity( name = "SourceEntry" )
	@Table( name = "ALTIBASE_VERSION_SOURCE" )
	public static class SourceEntry {
		@Id
		private Integer id;

		private String name;

		public SourceEntry() {
		}

		public SourceEntry(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
