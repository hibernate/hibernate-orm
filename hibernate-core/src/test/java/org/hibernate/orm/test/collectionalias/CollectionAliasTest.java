/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collectionalias;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Dave Stephan
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				TableBId.class,
				TableB.class,
				TableA.class,
				ATable.class
		}
)
@SessionFactory
public class CollectionAliasTest {

	@JiraKey(value = "HHH-7545")
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					ATable aTable = new ATable( 1 );
					TableB tableB = new TableB(
							new TableBId( 1, "a", "b" )
					);
					aTable.getTablebs().add( tableB );
					tableB.setTablea( aTable );
					session.persist( aTable );
				}
		);

		scope.inSession(
				session -> {
					ATable aTable = (ATable) session.createQuery(
							"select distinct	tablea from ATable tablea LEFT JOIN FETCH tablea.tablebs " )
							.uniqueResult();
					assertEquals( new Integer( 1 ), aTable.getFirstId() );
					assertEquals( 1, aTable.getTablebs().size() );
					TableB tableB = aTable.getTablebs().get( 0 );
					assertSame( aTable, tableB.getTablea() );
					assertEquals( new Integer( 1 ), tableB.getId().getFirstId() );
					assertEquals( "a", tableB.getId().getSecondId() );
					assertEquals( "b", tableB.getId().getThirdId() );
				}
		);
	}

}
