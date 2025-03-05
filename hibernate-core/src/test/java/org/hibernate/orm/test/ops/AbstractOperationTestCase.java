/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import org.hibernate.cfg.Environment;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SessionFactory(generateStatistics = true)
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/ops/Node.hbm.xml",
				"org/hibernate/orm/test/ops/Employer.hbm.xml",
				"org/hibernate/orm/test/ops/OptLockEntity.hbm.xml",
				"org/hibernate/orm/test/ops/OneToOne.hbm.xml",
				"org/hibernate/orm/test/ops/Competition.hbm.xml"
		}
)
@ServiceRegistry(
		settings = @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
)
public abstract class AbstractOperationTestCase {

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	protected void clearCounts(SessionFactoryScope scope) {
		getStatistics( scope ).clear();
	}

	protected void assertInsertCount(int expected, SessionFactoryScope scope) {
		int inserts = (int) getStatistics( scope ).getEntityInsertCount();
		assertThat( "unexpected insert count", inserts, is( expected ) );
	}

	protected void assertUpdateCount(int expected, SessionFactoryScope scope) {
		int updates = (int) getStatistics( scope ).getEntityUpdateCount();
		assertThat( "unexpected update counts", updates, is( expected ) );
	}

	protected void assertDeleteCount(int expected, SessionFactoryScope scope) {
		int deletes = (int) getStatistics( scope ).getEntityDeleteCount();
		assertThat( "unexpected delete counts", deletes, is( expected ) );
	}

	private StatisticsImplementor getStatistics(SessionFactoryScope scope) {
		return scope.getSessionFactory().getStatistics();
	}
}
