/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
public class CrossJoinTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SimpleEntity.class,
		};
	}

	@Test
	public void testSimpleCrossJoin() {
		inTransaction(
				session -> session.createQuery( "from SimpleEntity e1, SimpleEntity e2 where e1.id = e2.id and e1.someDate = {d '2018-01-01'}" ).list()
		);
	}
}
