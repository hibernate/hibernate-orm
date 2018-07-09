package org.hibernate.test.eagerjoin.onetomany;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import java.util.function.Consumer;

public abstract class OneToManyEagerJoinDiamondTest extends BaseCoreFunctionalTestCase {
	// TODO Reuse this for a bidirectional test case
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
