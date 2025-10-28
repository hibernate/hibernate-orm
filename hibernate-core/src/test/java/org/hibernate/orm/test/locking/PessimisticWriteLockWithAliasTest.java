/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

/**
 * @author Bin Chen (bin.chen@team.neustar)
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = { A.class, B.class })
@SessionFactory(useCollectingStatementInspector = true)
public class PessimisticWriteLockWithAliasTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var entityA = new A();
			session.persist( entityA );
			var entityB = new B( "foo" );
			entityB.setA( entityA );
			session.persist( entityB );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey("HHH-12866")
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgreSQLDialect.class)
	public void testSetLockModeWithAlias(SessionFactoryScope factoryScope) {
		var sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			//noinspection removal
			session.createQuery("select b from B b left join fetch b.a", B.class )
					.setLockMode( PESSIMISTIC_WRITE )
					.list();

			/*
			 * The generated SQL would be like: <pre> select b0_.id as id1_1_0_, a1_.id as id1_0_1_, b0_.a_id as
			 * a_id3_1_0_, b0_.b_value as b_value2_1_0_, a1_.a_value as a_value2_0_1_ from T_LOCK_B b0_ left outer join
			 * T_LOCK_A a1_ on b0_.a_id=a1_.id for update of b0_.id </pre>
			 */
			var lockingQuery = sqlCollector.getLastQuery();

			// attempt to get the alias that is specified in the from clause
			var fromTableAliasPattern = Pattern.compile( "from t_lock_b (\\S+)", CASE_INSENSITIVE | MULTILINE );
			var aliasGroup = fromTableAliasPattern.matcher( lockingQuery );
			Assertions.assertTrue( aliasGroup.find(), "Fail to locate alias in the from clause: " + lockingQuery );
			Assertions.assertTrue( lockingQuery.endsWith( " for update of " + aliasGroup.group( 1 ) + ".id" ) // Oracle
						|| lockingQuery.endsWith( " for no key update of " + aliasGroup.group( 1 ) ),
					"Actual query: " + lockingQuery ); // PostgreSQL
		} );
	}

}
