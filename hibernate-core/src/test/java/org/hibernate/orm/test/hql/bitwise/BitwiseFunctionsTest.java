/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.bitwise;

import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Brett Meyer
 */
@DomainModel(annotatedClasses = IntEntity.class)
@SessionFactory
public class BitwiseFunctionsTest {

	@Test @SkipForDialect(dialectClass = DerbyDialect.class)
	public void test(SessionFactoryScope scope) {
		IntEntity five = new IntEntity();
		five.setIntValue(5);
		scope.inTransaction(session -> session.persist(five));

		scope.inTransaction(
				session -> {
					assertEquals(session.createSelectionQuery(
									"select bitand(intValue,0) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 0 );
					assertEquals(session.createSelectionQuery(
									"select bitand(intValue,2) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 0 );
					assertEquals(session.createSelectionQuery(
									"select bitand(intValue,3) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 1 );
					assertEquals(session.createSelectionQuery(
									"select bitor(intValue,0) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 5 );
					assertEquals(session.createSelectionQuery(
									"select bitor(intValue,2) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 7 );
					assertEquals(session.createSelectionQuery(
									"select bitor(intValue,3) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 7 );
					assertEquals(session.createSelectionQuery(
									"select bitxor(intValue,3) from IntEntity", Integer.class )
							.uniqueResult().intValue(), 6 );
				}
		);
	}
}
