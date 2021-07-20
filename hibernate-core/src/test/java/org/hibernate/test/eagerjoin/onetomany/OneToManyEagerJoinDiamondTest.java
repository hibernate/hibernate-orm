/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.eagerjoin.onetomany;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import java.util.function.Consumer;

public abstract class OneToManyEagerJoinDiamondTest extends BaseCoreFunctionalTestCase {

	protected Statistics runInTransaction(Consumer<Session> function) {
		Session session = this.openSession();
		Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		session.beginTransaction();
		function.accept( session );
		session.getTransaction().commit();
		session.close();
		return session.getSessionFactory().getStatistics();
	}
}