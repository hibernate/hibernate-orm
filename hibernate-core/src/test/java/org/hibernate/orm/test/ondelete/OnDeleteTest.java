/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/ondelete/Person.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
public class OnDeleteTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(
			feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class,
			comment = "db/dialect does not support circular cascade delete constraints"
	)
	public void testJoinedSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Salesperson mark = new Salesperson();
					mark.setName( "Mark" );
					mark.setTitle( "internal sales" );
					mark.setSex( 'M' );
					mark.setAddress( "buckhead" );
					mark.setZip( "30305" );
					mark.setCountry( "USA" );

					Person joe = new Person();
					joe.setName( "Joe" );
					joe.setAddress( "San Francisco" );
					joe.setZip( "XXXXX" );
					joe.setCountry( "USA" );
					joe.setSex( 'M' );
					joe.setSalesperson( mark );
					mark.getCustomers().add( joe );

					session.persist( mark );

					session.getTransaction().commit();

					final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

					assertThat( statistics.getEntityInsertCount(), is( 2L ) );
					assertThat( statistics.getPrepareStatementCount(), is( 5L ) );

					statistics.clear();

					Transaction t = session.beginTransaction();
					session.remove( mark );
					t.commit();

					assertThat( statistics.getEntityDeleteCount(), is( 2L ) );
					if ( scope.getSessionFactory().getJdbcServices().getDialect().supportsCascadeDelete() ) {
						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					}

					session.beginTransaction();
					List names = session.createQuery( "select name from Person" ).list();
					assertTrue( names.isEmpty() );
				}
		);

	}

}
