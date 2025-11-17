/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.notfound;

import java.io.Serializable;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(
		annotatedClasses = { NotFoundTest.Coin.class, NotFoundTest.Currency.class }
)
@SessionFactory
public class NotFoundTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final Currency euro = new Currency();
		euro.setName( "Euro" );

		final Coin fiveCents = new Coin();
		fiveCents.setName( "Five cents" );
		fiveCents.setCurrency( euro );

		scope.inTransaction( session -> {
			session.persist( euro );
			session.persist( fiveCents );
		} );

		scope.inTransaction( session -> {
			Currency _euro = session.get( Currency.class, euro.getId() );
			session.remove( _euro );
		} );

		scope.inTransaction( session -> {
			Coin _fiveCents = session.get( Coin.class, fiveCents.getId() );
			assertNull( _fiveCents.getCurrency() );
			session.remove( _fiveCents );
		} );
	}

	@Entity(name = "Coin")
	public static class Coin {

		private Integer id;

		private String name;

		private Currency currency;

		@Id
		@GeneratedValue
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

		@ManyToOne
		@JoinColumn(name = "currency", referencedColumnName = "name")
		@NotFound(action = NotFoundAction.IGNORE)
		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}
	}

	@Entity(name = "Currency")
	public static class Currency implements Serializable {

		private Integer id;

		private String name;

		@Id
		@GeneratedValue
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

}
