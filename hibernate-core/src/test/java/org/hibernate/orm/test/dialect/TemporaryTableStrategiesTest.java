/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests the stock temporary-table strategies selected by maintained Dialects.
///
/// @author Steve Ebersole
public class TemporaryTableStrategiesTest {
	@Test
	void stockStrategiesAreStableAndSharedByMaintainedDialects() {
		assertThat( TemporaryTableStrategies.db2Global() )
				.isSameAs( TemporaryTableStrategies.db2Global() )
				.isSameAs( new DB2Dialect().getGlobalTemporaryTableStrategy() );
		assertThat( TemporaryTableStrategies.hsqlLocal() )
				.isSameAs( new HSQLDialect().getLocalTemporaryTableStrategy() );
		assertThat( TemporaryTableStrategies.mysqlLocal() )
				.isSameAs( new MySQLDialect().getLocalTemporaryTableStrategy() );
		assertThat( TemporaryTableStrategies.oracleLocal() )
				.isSameAs( new OracleDialect().getLocalTemporaryTableStrategy() );
		assertThat( TemporaryTableStrategies.sqlServerLocal() )
				.isSameAs( new SQLServerDialect().getLocalTemporaryTableStrategy() );
		assertThat( new HANADialect().getGlobalTemporaryTableStrategy() )
				.isSameAs( StandardGlobalTemporaryTableStrategy.INSTANCE );
	}
}
