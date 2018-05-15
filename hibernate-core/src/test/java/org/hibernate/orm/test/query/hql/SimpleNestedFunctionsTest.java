/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleNestedFunctionsTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testSubstrInsideConcat() {
		sessionFactoryScope().inTransaction(
				session -> {
					List<Object> results = session.createQuery(
							"select concat('111', concat('222222', '1')) from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( "1112222221" ) );
				} );
	}

	@BeforeEach
	public void setUp() {
		sessionFactoryScope().inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							null
					);
					session.save( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							null
					);
					session.save( second_entity );

				} );
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "from SimpleEntity e" )
							.list()
							.forEach( simpleEntity -> session.delete( simpleEntity ) );
				} );
	}
}
