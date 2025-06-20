/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazydetachedpersist;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				LazyPersistWithDetachedAssociationTest.Address.class,
				LazyPersistWithDetachedAssociationTest.Person.class,
		}
)
@SessionFactory
@ServiceRegistry(
		settings = { @Setting(name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "false") }
)
public class LazyPersistWithDetachedAssociationTest {

	@BeforeEach
	public void setUpData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Address address = new Address();
			address.setId( 1L );
			address.setContent( "21 Jump St" );
			session.persist( address );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-3846")
	public void testDetachedAssociationOnPersisting(SessionFactoryScope scope) {
		Address loadedAddress = scope.fromTransaction(
				session -> {
					// first load the address
					Address _loadedAddress = session.getReference(
							Address.class,
							1L
					);
					assertNotNull( _loadedAddress );
					return _loadedAddress;
				}
		);

		scope.inTransaction( session -> {
			session.get( Address.class, 1L );

			Person person = new Person();
			person.setId( 1L );
			person.setName( "Johnny Depp" );
			person.setAddress( loadedAddress );

			session.persist( person );
		} );
	}

	@Entity(name = "Address")
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

	@Entity(name = "Person")
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

		@ManyToOne(fetch = FetchType.LAZY)
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
