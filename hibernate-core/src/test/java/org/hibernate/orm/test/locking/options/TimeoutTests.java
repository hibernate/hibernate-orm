/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Timeout;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.RowLockStrategy;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class TimeoutTests {

	@Test
	@DomainModel(annotatedClasses = TimeoutTests.Lockable.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsRealQueryLockTimeouts.class )
	void testArgExecution(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Lockable.class, 1, PESSIMISTIC_WRITE, Timeout.milliseconds( 2_000 ) );
			}
			catch (Exception ignore) {
			}

			final String expectedLockFragment = determineExpectedLockFragment(
					new LockOptions( PESSIMISTIC_WRITE, Timeout.milliseconds( 2_000 ) ),
					session.getDialect()
			);
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( expectedLockFragment );
		} );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = SpecHints.HINT_SPEC_LOCK_TIMEOUT, value = "2000"))
	@DomainModel(annotatedClasses = TimeoutTests.Lockable.class)
	@SessionFactory(useCollectingStatementInspector = true)
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsRealQueryLockTimeouts.class )
	void testFactoryHintExecution(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();
		factoryScope.inTransaction( (session) -> {
			try {
				session.find( Lockable.class, 1, PESSIMISTIC_WRITE );
			}
			catch (Exception ignore) {
			}

			final String expectedLockFragment = determineExpectedLockFragment(
					new LockOptions( PESSIMISTIC_WRITE, Timeout.milliseconds( 2_000 ) ),
					session.getDialect()
			);
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( expectedLockFragment );
		} );
	}

	private static String determineExpectedLockFragment(
			LockOptions lockOptions,
			Dialect dialect) {
		final LockingSupport.Metadata lockingMetadata = dialect.getLockingSupport().getMetadata();
		final PessimisticLockStyle pessimisticLockStyle = lockingMetadata.getPessimisticLockStyle();
		final RowLockStrategy rowLockStrategy = lockingMetadata.getWriteRowLockStrategy();

		if ( pessimisticLockStyle == PessimisticLockStyle.TABLE_HINT ) {
			// T-SQL
			return dialect.appendLockHint( lockOptions, "l1_0" );
		}
		else {
			if ( pessimisticLockStyle == PessimisticLockStyle.NONE || rowLockStrategy == RowLockStrategy.NONE ) {
				return dialect.getWriteLockString( lockOptions.getTimeout() );
			}
			else if ( rowLockStrategy == RowLockStrategy.TABLE ) {
				return dialect.getWriteLockString( "l1_0", lockOptions.getTimeout() );
			}
			else {
				assert rowLockStrategy == RowLockStrategy.COLUMN;
				return dialect.getWriteLockString( "l1_0.id", lockOptions.getTimeout() );
			}
		}
	}

	@Entity(name="Lockable")
	@Table(name="Lockable")
	public static class Lockable {
		@Id
		private Integer id;
		private String name;

		public Lockable() {
		}

		public Lockable(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
