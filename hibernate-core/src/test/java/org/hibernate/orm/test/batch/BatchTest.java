/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This is how to do batch processing in Hibernate. Remember to enable JDBC batch updates, or this test will take a
 * VeryLongTime!
 *
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name= AvailableSettings.STATEMENT_BATCH_SIZE, value = "20")
})
@DomainModel(xmlMappings = "org/hibernate/orm/test/batch/DataPoint.xml")
@SessionFactory
public class BatchTest {

	@Test
	public void testBatchInsertUpdate(SessionFactoryScope factoryScope) {
		long start = System.currentTimeMillis();
		final int N = 5000; //26 secs with batch flush, 26 without
		//final int N = 100000; //53 secs with batch flush, OOME without
		//final int N = 250000; //137 secs with batch flush, OOME without
		int batchSize = factoryScope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( N, batchSize, factoryScope );
		System.out.println( System.currentTimeMillis() - start );
	}

	@Test
	public void testBatchInsertUpdateSizeEqJdbcBatchSize(SessionFactoryScope factoryScope) {
		int batchSize = factoryScope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize, factoryScope );
	}

	@Test
	public void testBatchInsertUpdateSizeLtJdbcBatchSize(SessionFactoryScope factoryScope) {
		int batchSize = factoryScope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize - 1, factoryScope );
	}

	@Test
	public void testBatchInsertUpdateSizeGtJdbcBatchSize(SessionFactoryScope factoryScope) {
		int batchSize = factoryScope.getSessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize + 1, factoryScope );
	}

	public void doBatchInsertUpdate(int nEntities, int nBeforeFlush, SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.IGNORE );
			for ( int i = 0; i < nEntities; i++ ) {
				DataPoint dp = new DataPoint();
				dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, RoundingMode.DOWN ) );
				dp.setY( BigDecimal.valueOf( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, RoundingMode.DOWN ) );
				session.persist( dp );
				if ( ( i + 1 ) % nBeforeFlush == 0 ) {
					session.flush();
					session.clear();
				}
			}
		} );

		factoryScope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.IGNORE );
			int i = 0;
			try (ScrollableResults sr = session.createQuery( "from DataPoint dp order by dp.x asc" )
					.scroll( ScrollMode.FORWARD_ONLY )) {
				while ( sr.next() ) {
					DataPoint dp = (DataPoint) sr.get();
					dp.setDescription( "done!" );
					if ( ++i % nBeforeFlush == 0 ) {
						session.flush();
						session.clear();
					}
				}
			}
		} );

		factoryScope.inTransaction( (session) -> {
			session.setCacheMode( CacheMode.IGNORE );
			int i = 0;

			try (ScrollableResults sr = session.createQuery( "from DataPoint dp order by dp.x asc" )
					.scroll( ScrollMode.FORWARD_ONLY )) {
				while ( sr.next() ) {
					DataPoint dp = (DataPoint) sr.get();
					session.remove( dp );
					if ( ++i % nBeforeFlush == 0 ) {
						session.flush();
						session.clear();
					}
				}
			}
		} );
	}
}
