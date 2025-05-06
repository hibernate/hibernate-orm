/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;


import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.*;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Aber Tian
 */
@JiraKey("HHH-16054")
public class JoinedSubclassWithIdClassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				BaseEntity.class,
				ConcreteEntity.class
		};
	}

	@Test
	public void testJoinedSubClassWithIdClassComposeKey() {

		ConcreteEntity entity = new ConcreteEntity();
		entity.setId( 1L );
		entity.setDealer( "dealer" );
		entity.setName( "aber" );
		entity.setAge( 18 );

		Pk pk = new Pk();
		pk.id = 1L;
		pk.dealer = "dealer";

		doInHibernate( this::sessionFactory, session -> {
			session.merge( entity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			entity.setName( "tian" );
			session.merge( entity );
			BaseEntity baseEntity = session.find( BaseEntity.class, pk );
			assertThat( baseEntity.name, is( "tian" ) );
		} );

	}

	public static class Pk implements Serializable {
		private long id;

		private String dealer;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getDealer() {
			return dealer;
		}

		public void setDealer(String dealer) {
			this.dealer = dealer;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Pk pk = (Pk) o;
			return id == pk.id && Objects.equals( dealer, pk.dealer );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, dealer );
		}
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@IdClass(Pk.class)
	public static abstract class BaseEntity {
		@Id
		private long id;

		@Id
		private String dealer;

		private String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getDealer() {
			return dealer;
		}

		public void setDealer(String dealer) {
			this.dealer = dealer;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ConcreteEntity")
	public static class ConcreteEntity extends BaseEntity {

		private int age;

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
