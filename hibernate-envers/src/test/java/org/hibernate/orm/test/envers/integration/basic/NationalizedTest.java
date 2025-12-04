/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.envers.Audited;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey(value = "HHH-19976")
@DomainModel(annotatedClasses = {NationalizedTest.NationalizedEntity.class})
@SessionFactory
@SkipForDialect(dialectClass = OracleDialect.class)
@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true, reason = "@Lob field in HQL predicate fails with error about text = bigint")
@SkipForDialect(dialectClass = HANADialect.class, matchSubTypes = true, reason = "HANA doesn't support comparing LOBs with the = operator")
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Sybase doesn't support comparing LOBs with the = operator")
@SkipForDialect(dialectClass = DB2Dialect.class, matchSubTypes = true, reason = "DB2 jdbc driver doesn't support setNString")
@SkipForDialect(dialectClass = DerbyDialect.class, matchSubTypes = true, reason = "Derby jdbc driver doesn't support setNString")
public class NationalizedTest {

	@Test
	public void testMetadataBindings(DomainModelScope scope) {
		final var domainModel = scope.getDomainModel();

		assertThrows( AssertionFailedError.class, () -> {
			final Table auditTable = domainModel.getEntityBinding( NationalizedEntity.class.getName() + "_AUD" )
					.getTable();

			final org.hibernate.mapping.Column colDef = auditTable.getColumn( toIdentifier( "nationalizedString" ) );
			assertEquals( StandardBasicTypes.NSTRING.getName(), colDef.getTypeName() );
		} );
	}

	@Entity(name = "NationalizedEntity")
	@Audited
	public static class NationalizedEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Nationalized
		private String nationalizedString;
	}
}
