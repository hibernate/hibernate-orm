/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype.inet;

import java.util.List;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgreSQLInetTypesOtherTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.setId( 1L );
			event.setIp( "192.168.0.123/24" );

			entityManager.persist( event );
		} );
	}

	@Test
	public void testJPQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Inet> inets = entityManager.createQuery(
				"select e.ip " +
				"from Event e " +
				"where e.id = :id" )
			.setParameter( "id", 1L )
			.getResultList();

			assertEquals( 1, inets.size() );
			assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}

	@Test
	public void testNativeSQLAddScalar() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Inet> inets = entityManager.createNativeQuery(
				"select e.ip as ip " +
				"from Event e " +
				"where e.id = :id" )
			.setParameter( "id", 1L )
			.unwrap( NativeQuery.class )
			.addScalar( "ip", InetType.INSTANCE )
			.getResultList();

			assertEquals( 1, inets.size() );
			assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}

}
