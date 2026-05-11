/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondelete;

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
@DomainModel(xmlMappings = "org/hibernate/orm/test/ondelete/Person.hbm.xml")
@SessionFactory(generateStatistics = true)
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
		var statistics = scope.getSessionFactory().getStatistics();

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

					assertThat( statistics.getEntityInsertCount(), is( 2L ) );

					statistics.clear();

					session.beginTransaction();
					session.remove( mark );
					session.getTransaction().commit();

					assertThat( statistics.getEntityDeleteCount(), is( 2L ) );
					if ( scope.getSessionFactory().getJdbcServices().getDialect().supportsCascadeDelete() ) {
						assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					}

					session.beginTransaction();
					var names = session.createQuery( "select name from Person", String.class ).list();
					assertTrue( names.isEmpty() );
				}
		);

	}

}
