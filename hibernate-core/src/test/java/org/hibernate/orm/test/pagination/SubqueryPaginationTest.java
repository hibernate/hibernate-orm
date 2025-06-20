/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EnumValue;
import org.hibernate.testing.orm.domain.gambit.SimpleComponent;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian Beikov
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsOrderByInCorrelatedSubquery.class)
public class SubqueryPaginationTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfLists entityContainingLists = new EntityOfLists( 1, "first" );

					entityContainingLists.addBasic( "abc" );
					entityContainingLists.addBasic( "def" );
					entityContainingLists.addBasic( "ghi" );

					entityContainingLists.addConvertedEnum( EnumValue.TWO );

					entityContainingLists.addEnum( EnumValue.ONE );
					entityContainingLists.addEnum( EnumValue.THREE );

					entityContainingLists.addComponent( new SimpleComponent( "first-a1", "first-another-a1" ) );
					entityContainingLists.addComponent( new SimpleComponent( "first-a2", "first-another-a2" ) );
					entityContainingLists.addComponent( new SimpleComponent( "first-a3", "first-another-a2" ) );

					session.persist( entityContainingLists );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "Generates nested correlated subquery which is not supported in that version")
	public void testLimitInSubquery(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<EntityOfLists> list = session.createQuery(
							"from EntityOfLists e where 'abc' = (select basic from e.listOfBasics basic order by basic limit 1)",
							EntityOfLists.class
					).list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
	@SkipForDialect(dialectClass = OracleDialect.class, majorVersion = 11, reason = "Generates nested correlated subquery which is not supported in that version")
	@SkipForDialect(dialectClass = DB2Dialect.class, majorVersion = 10, reason = "Generates nested correlated subquery which is not supported in that version")
	public void testLimitAndOffsetInSubquery(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					List<EntityOfLists> list = session.createQuery(
							"from EntityOfLists e where 'def' = (select basic from e.listOfBasics basic order by basic limit 1 offset 1)",
							EntityOfLists.class
					).list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}
}
