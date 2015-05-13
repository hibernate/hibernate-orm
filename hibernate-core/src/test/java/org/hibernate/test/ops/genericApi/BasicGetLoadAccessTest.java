/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
