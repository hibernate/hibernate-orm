/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ops.genericApi;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class BasicGetLoadAccessTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( User.class );
	}

	@Entity( name = "User" )
	@Table( name = "user" )
	public static class User {
		private Integer id;
		private String name;

		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue( generator = "increment" )
		@GenericGenerator( name = "increment", strategy = "increment" )
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Test
	public void testIt() {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create a row
		Session s = openSession();
		s.beginTransaction();
		s.save( new User( "steve" ) );
		s.getTransaction().commit();
		s.close();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `get` access
		s = openSession();
		s.beginTransaction();
		User user = s.get( User.class, 1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.get( User.class, 1, LockMode.PESSIMISTIC_WRITE );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.get( User.class, 1, LockOptions.UPGRADE );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.byId( User.class ).load( 1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.byId( User.class ).with( LockOptions.UPGRADE ).load( 1 );
		s.getTransaction().commit();
		s.close();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// test `load` access
		s = openSession();
		s.beginTransaction();
		user = s.load( User.class, 1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.load( User.class, 1, LockMode.PESSIMISTIC_WRITE );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.load( User.class, 1, LockOptions.UPGRADE );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.byId( User.class ).getReference( 1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		user = s.byId( User.class ).with( LockOptions.UPGRADE ).getReference( 1 );
		s.getTransaction().commit();
		s.close();
	}
}
