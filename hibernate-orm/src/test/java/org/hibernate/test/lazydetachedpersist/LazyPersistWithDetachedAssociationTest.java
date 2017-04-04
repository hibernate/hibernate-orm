/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazydetachedpersist;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

public class LazyPersistWithDetachedAssociationTest
		extends BaseCoreFunctionalTestCase {


	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Address.class,
				Person.class,
		};
	}

	@Before
	public void setUpData() {
		doInHibernate( this::sessionFactory, session -> {
			Address address = new Address();
			address.setId( 1L );
			address.setContent( "21 Jump St" );
			session.persist( address );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-3846")
	public void testDetachedAssociationOnPersisting() {
		sessionFactory().getStatistics().clear();

		Address loadedAddress = doInHibernate(
				this::sessionFactory,
				session -> {
					// first load the address
					Address _loadedAddress = session.load(
							Address.class,
							1L
					);
					assertNotNull( _loadedAddress );
					return _loadedAddress;
				}
		);

		doInHibernate( this::sessionFactory, session -> {
			session.get( Address.class, 1L );

			Person person = new Person();
			person.setId( 1L );
			person.setName( "Johnny Depp" );
			person.setAddress( loadedAddress );

			session.persist( person );
		} );
	}

	@Entity
	@Table(name = "eg_sbt_address")
	public static class Address {

		private Long id;
		private String content;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Basic
		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	@Entity
	@Table(name = "eg_sbt_person")
	public static class Person {

		private Long id;
		private Address address;
		private String name;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY, cascade = {})
		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		@Basic
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}
}
