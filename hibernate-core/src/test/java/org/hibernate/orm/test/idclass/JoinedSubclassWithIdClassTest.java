/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Aber Tian
 */
@JiraKey("HHH-16054")
@DomainModel(annotatedClasses = {
		JoinedSubclassWithIdClassTest.BaseEntity.class,
		JoinedSubclassWithIdClassTest.ConcreteEntity.class
})
@SessionFactory
public class JoinedSubclassWithIdClassTest {
	@Test
	public void testJoinedSubClassWithIdClassComposeKey(SessionFactoryScope factoryScope) {
		var created = factoryScope.fromTransaction( (session) -> {
			var entity = new ConcreteEntity();
			entity.setId( 1L );
			entity.setDealer( "dealer" );
			entity.setName( "aber" );
			entity.setAge( 18 );

			return session.merge( entity );
		} );

		factoryScope.inTransaction( (session) -> {
			created.setName( "tian" );
			session.merge( created );

			var pk = new Pk();
			pk.id = 1L;
			pk.dealer = "dealer";

			var baseEntity = session.find( BaseEntity.class, pk );
			assertThat( baseEntity.name ).isEqualTo( "tian" );
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
