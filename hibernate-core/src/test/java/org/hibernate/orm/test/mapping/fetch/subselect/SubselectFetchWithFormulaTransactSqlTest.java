/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.mapping.fetch.subselect.SubselectFetchWithFormulaTest.prepareTestData;
import static org.hibernate.orm.test.mapping.fetch.subselect.SubselectFetchWithFormulaTest.verify;

@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(SybaseDialect.class)
@DomainModel(xmlMappings = {"mappings/subselectfetch/name-tsql.xml", "mappings/subselectfetch/value.xml"})
@SessionFactory(useCollectingStatementInspector = true)
@FailureExpected(reason = "https://hibernate.atlassian.net/browse/HHH-19316")
public class SubselectFetchWithFormulaTransactSqlTest {

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		prepareTestData( factoryScope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void checkSubselectWithFormula(DomainModelScope modelScope, SessionFactoryScope factoryScope) {
		verify( modelScope, factoryScope );
	}

}
