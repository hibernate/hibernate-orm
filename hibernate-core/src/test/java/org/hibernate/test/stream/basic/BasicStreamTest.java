/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stream.basic;

import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Steve Ebersole
 */
public class BasicStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( MyEntity.class );
	}

	@Test
	public void basicStreamTest() {
		Session session = openSession();
		session.getTransaction().begin();

		// mainly we want to make sure that closing the Stream releases the ScrollableResults too
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( false ) );
		final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream();
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( true ) );
		stream.forEach( System.out::println );
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( true ) );
		stream.close();
		assertThat( ( (SessionImplementor) session ).getJdbcCoordinator().getLogicalConnection().getResourceRegistry().hasRegisteredResources(), is( false ) );

		session.getTransaction().commit();
		session.close();
	}

	@Entity(name = "MyEntity")
	@Table(name="MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

}
