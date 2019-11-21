/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.exec;

import java.util.Calendar;
import java.util.List;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;

import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class FunctionTests extends BaseSqmUnitTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	@FailureExpected( reason = "Function support not yet implemented" )
	public void testSubstrInsideConcat() {
		inTransaction(
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
		inTransaction(
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
		inTransaction(
				session -> {
					session.createQuery( "delete SimpleEntity" ).executeUpdate();
				}
		);
	}

}
