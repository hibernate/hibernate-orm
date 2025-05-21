/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.options;

import jakarta.persistence.LockModeType;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.transaction.TransactionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = Detail.class)
@SessionFactory(useCollectingStatementInspector = true)
public class ScopeAndSecondaryTableTests {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new Detail( 1, "heeby", "jeeby" ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature=DialectFeatureChecks.SupportsLockingJoins.class, comment = "Come back and rework this to account for follow-on testing")
	void simpleTest(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.find( Detail.class, 1 );

			session.clear();
			sqlCollector.clear();
			session.find( Detail.class, 1, LockModeType.PESSIMISTIC_WRITE );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			Helper.checkSql( sqlCollector.getSqlQueries().get( 0 ), session.getDialect(), Tables.DETAILS, Tables.SUPPLEMENTALS );
			TransactionUtil.updateTable( factoryScope, Tables.DETAILS.getTableName(), "name", true );
			TransactionUtil.updateTable( factoryScope, Tables.SUPPLEMENTALS.getTableName(), "txt", true );
		} );
	}

	enum Tables implements Helper.TableInformation {
		DETAILS,
		SUPPLEMENTALS;


		@Override
		public String getTableName() {
			return switch ( this ) {
				case DETAILS -> "details";
				case SUPPLEMENTALS -> "supplementals";
			};
		}

		@Override
		public String getTableAlias() {
			return switch ( this ) {
				case DETAILS -> "d1_0";
				case SUPPLEMENTALS -> "d1_1";
			};
		}

		@Override
		public String getKeyColumnName() {
			return switch ( this ) {
				case DETAILS -> "id";
				case SUPPLEMENTALS -> "detail_fk";
			};
		}
	}
}
