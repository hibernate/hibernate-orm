/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.exec;

import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntity.class )
@SessionFactory
public class CrossJoinTest {
	@Test
	public void testSimpleCrossJoin(SessionFactoryScope scope) {
		final String QRY_STRING = "from SimpleEntity e1, SimpleEntity e2 where e1.id = e2.id and e1.someDate = {ts '2018-01-01 00:00:00'}";
		scope.inTransaction(
				session -> {
					session.createQuery( QRY_STRING, Object[].class ).list();
				}
		);
	}
}
