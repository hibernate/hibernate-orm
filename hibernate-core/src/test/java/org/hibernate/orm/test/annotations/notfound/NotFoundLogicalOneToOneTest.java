/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.notfound;

import java.io.Serializable;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Emmanuel Bernard
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = { NotFoundLogicalOneToOneTest.Coin.class, NotFoundLogicalOneToOneTest.Currency.class }
)
@SessionFactory
public class NotFoundLogicalOneToOneTest {

	@Test
	public void testLogicalOneToOne(SessionFactoryScope scope) {
		Currency euro = new Currency();
		euro.setName( "Euro" );
		Coin fiveC = new Coin();
		fiveC.setName( "Five cents" );
		fiveC.setCurrency( euro );

		scope.inTransaction(
				session -> {
					session.persist( euro );
					session.persist( fiveC );
				}
		);

		scope.inTransaction(
				session ->
						session.delete( euro )
		);

		scope.inTransaction(
				session -> {
					Coin coin = session.get( Coin.class, fiveC.getId() );
					assertNull( coin.getCurrency() );

					session.delete( coin );
				}
		);
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

		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
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
