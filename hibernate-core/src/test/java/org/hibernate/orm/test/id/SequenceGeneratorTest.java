/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/id/Person.hbm.xml"
)
@SessionFactory(useCollectingStatementInspector = true)
public class SequenceGeneratorTest {


	/**
	 * This seems a little trivial, but we need to guarantee that all Dialects start their sequences on a non-0 value.
	 */
	@Test
	@JiraKey(value = "HHH-8814")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
	@SkipForDialect(
			dialectClass = SQLServerDialect.class,
			majorVersion = 11,
			reason = "SQLServer2012Dialect initializes sequence to minimum value (e.g., Long.MIN_VALUE; Hibernate assumes it is uninitialized.",
			matchSubTypes = true
	)
	public void testStartOfSequence(SessionFactoryScope scope) {
		final Person person = new Person();
		scope.inTransaction(
				session -> {
					session.persist( person );
				}
		);

		assertTrue( person.getId() > 0 );
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		assertTrue( statementInspector.getSqlQueries()
							.stream()
							.filter( sql -> sql.contains( "product_sequence" ) )
							.findFirst()
							.isPresent() );
	}

}
