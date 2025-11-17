/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsIdentityColumns;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialectFeature(feature = SupportsIdentityColumns.class, jiraKey = "HHH-9271")
@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 12, versionMatchMode = VersionMatchMode.SAME_OR_OLDER, reason = "Oracle and identity column: java.sql.Connection#prepareStatement(String sql, int columnIndexes[]) does not work with quoted table names and/or quoted columnIndexes")
@DomainModel(
		annotatedClasses = {
				QuotedIdentifierTest.QuotedIdentifier.class
		}
)
@SessionFactory
public class QuotedIdentifierTest {

	@Test
	public void testDirectIdPropertyAccess(SessionFactoryScope scope) {
		QuotedIdentifier quotedIdentifier = new QuotedIdentifier();
		scope.inTransaction( session -> {
			quotedIdentifier.timestamp = System.currentTimeMillis();
			quotedIdentifier.from = "HHH-9271";
			session.persist( quotedIdentifier );
		} );

		scope.inTransaction( session -> {
			QuotedIdentifier result = session.get( QuotedIdentifier.class, quotedIdentifier.index );
			assertNotNull( result );
		} );
	}

	@Entity(name = "QuotedIdentifier")
	@Table(name = "`table`")
	public static class QuotedIdentifier {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "`index`")
		private int index;

		@Column(name = "`timestamp`")
		private long timestamp;

		@Column(name = "`from`")
		private String from;
	}
}
