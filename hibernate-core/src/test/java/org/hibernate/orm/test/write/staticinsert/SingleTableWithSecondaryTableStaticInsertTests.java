/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write.staticinsert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.DomesticVendor;
import org.hibernate.testing.orm.domain.retail.ForeignVendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SingleTableWithSecondaryTableStaticInsertTests.DomainAndFactory
public class SingleTableWithSecondaryTableStaticInsertTests {
	@Test
	@DomainAndFactory
	@ServiceRegistry(
			settings = {
					@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
					@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "-1")
			}
	)
	public void unBatchedSingleTableWithSecondaryTableTest(SessionFactoryScope scope) {
		verify( scope, 2 );
	}

	@Test
	@DomainAndFactory
	@ServiceRegistry(
			settings = {
					@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
					@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5")
			}
	)
	public void batchedSingleTableWithSecondaryTableTest(SessionFactoryScope scope) {
		verify( scope, 2 );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final int count = session.createMutationQuery( "delete Vendor" ).executeUpdate();
			assertThat( count ).isEqualTo( 2 );
		} );
	}

	private void verify(SessionFactoryScope scope, int expectedPrepCount) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( (session) -> {
			session.persist( new DomesticVendor( 1, "Acme Anvil Inc.", "Acme Worldwide") );
			session.persist( new ForeignVendor( 2, "Acme Train Inc.", "Acme Worldwide") );
		} );

		// supplemental details are null, so we should have no inserts for the secondary table
		assertThat( statementInspector.getSqlQueries() ).hasSize( expectedPrepCount );
		assertThat( statistics.getPrepareStatementCount() ).isEqualTo( expectedPrepCount );

		scope.inTransaction( (session) -> {
			final Long count = session
					.createSelectionQuery( "select count(1) from Vendor", Long.class )
					.getSingleResult();

			assertThat( count ).isEqualTo( 2 );
		} );
	}

//	@Test
//	@SimpleStaticInsertTests.DomainAndFactory
//	public void simpleSingleTableWithSecondaryTableTest2(SessionFactoryScope scope) {
//		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
//		statementInspector.clear();
//
//		scope.inTransaction( (session) -> {
//			session.persist( new DomesticVendor( 1, "Acme Anvil Inc.", "Acme Worldwide", "supplemental details - domestic" ) );
//			session.persist( new ForeignVendor( 2, "Acme Train Inc.", "Acme Worldwide", "supplemental details - foreign" ) );
//		} );
//
//		// supplemental details are non-null, so we should have inserts for the secondary table
//		assertThat( statementInspector.getSqlQueries() ).hasSize( 4 );
//	}

	@Target({ ElementType.TYPE, ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@DomainModel( standardModels = StandardDomainModel.RETAIL )
	@SessionFactory( useCollectingStatementInspector = true )
	@interface DomainAndFactory {}
}
