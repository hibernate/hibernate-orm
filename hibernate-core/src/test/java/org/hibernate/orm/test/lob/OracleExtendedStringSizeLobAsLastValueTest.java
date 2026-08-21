/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jira("https://hibernate.atlassian.net/browse/HHH-20499")
@DomainModel(annotatedClasses = OracleExtendedStringSizeLobAsLastValueTest.LobAsLastValueEntity.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.OracleExtendedStringSize.class)
public class OracleExtendedStringSizeLobAsLastValueTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testInsertLobAsLastValue(SessionFactoryScope scope) {
		// To reproduce ORA-24816, we must bind an 8K value for a clob column through the `setString` JDBC API,
		// followed by binding an 8K value for a non-clob column through the `setString` JDBC API.
		// If the clob column parameter is bound via `setClob` or `setBytes` JDBC APIs, there is no problem.
		int wrapperSize = 12;
		// Need to bind a value of size 8K+
		String lob = "{\"value\":\"" + "a".repeat( 8192 - wrapperSize ) + "\"}";
		scope.inTransaction( session -> {
			// This insert will fail on Oracle when the lob isn't put as last field
			final LobAsLastValueEntity entity = new LobAsLastValueEntity(1L, lob );
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			final LobAsLastValueEntity entity = session.find( LobAsLastValueEntity.class, 1L );
			assertEquals( lob, entity.column0 );
			assertEquals( lob, entity.column1 );
		});
	}

	@Entity(name = "LobAsLastValueEntity")
	public static class LobAsLastValueEntity {
		// The bug in HHH-20499 was that LONG32VARCHAR uses the clob DDL type, but binds via the `setString` JDBC API.
		@JdbcTypeCode(SqlTypes.LONG32VARCHAR)
//		@JdbcTypeCode(SqlTypes.LONGVARCHAR) // This will use VARCHAR2(32600), which is fine
//		@JdbcTypeCode(SqlTypes.JSON) // This will bind through `setBytes`, which is also fine
//		@Lob // This will bind through `setClob`, which is actually fine, so will not cause an error
//		@Column(length = Length.LONG32) // This will mark the column as CLOB, so will bind last
//		@Column(length = Length.LONG16) // This will use VARCHAR2(32767) when MAX_STRING_SIZE=EXTENDED, otherwise mark as CLOB so will bind last
//		@Column(length = Length.LONG) // Same as LONG16
		private String column0;
		@Column(length = 8192)
		private String column1;
		@Id
		private Long id;

		public LobAsLastValueEntity() {
		}

		public LobAsLastValueEntity(Long id, String lob) {
			this.id = id;
			this.column0 = lob;
			this.column1 = lob;
		}
	}
}
