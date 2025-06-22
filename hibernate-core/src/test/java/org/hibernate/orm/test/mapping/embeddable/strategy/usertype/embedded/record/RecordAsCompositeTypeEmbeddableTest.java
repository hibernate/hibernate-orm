/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.record;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.annotations.CompositeType;
import org.hibernate.metamodel.spi.ValueAccess;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.hibernate.usertype.CompositeUserType;

import org.javamoney.moneta.FastMoney;

import org.junit.Test;

import javax.money.MonetaryAmount;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class RecordAsCompositeTypeEmbeddableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { RecordAsCompositeTypeEmbeddableEntity.class };
	}

	@Test
	public void test() {
		MonetaryAmount amount = FastMoney.of( 1, "BRL" );

		RecordAsCompositeTypeEmbeddableEntity entity = new RecordAsCompositeTypeEmbeddableEntity();
		entity.setAmount( amount );
		entity.setId( 1L );

		inTransaction( session -> {
			session.persist( entity );
		} );

		inTransaction( session -> {
			RecordAsCompositeTypeEmbeddableEntity result = session.find(
					RecordAsCompositeTypeEmbeddableEntity.class,
					1L
			);
			assertEquals( result.getAmount(), entity.getAmount() );
		} );
	}

	public static class MonetaryAmountType implements CompositeUserType<MonetaryAmount> {
		public record MonetaryAmountMapper(
				BigDecimal amount,
				String currency
		) {
		}

		;

		public MonetaryAmountType() {
		}

		@Override
		public Object getPropertyValue(MonetaryAmount component, int property) throws HibernateException {
			//Alphabetical
			return switch ( property ) {
				case 0 -> component.getNumber().numberValueExact( BigDecimal.class );
				case 1 -> component.getCurrency().getCurrencyCode();
				default -> null;
			};
		}

		@Override
		public MonetaryAmount instantiate(ValueAccess values) {
			//Alphabetical
			BigDecimal amount = values.getValue( 0, BigDecimal.class );
			String currency = values.getValue( 1, String.class );
			return FastMoney.of( amount, currency );
		}

		@Override
		public Class<?> embeddable() {
			return MonetaryAmountMapper.class;
		}

		@Override
		public Class<MonetaryAmount> returnedClass() {
			return MonetaryAmount.class;
		}

		@Override
		public boolean equals(MonetaryAmount x, MonetaryAmount y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(MonetaryAmount x) {
			return x.hashCode();
		}

		@Override
		public MonetaryAmount deepCopy(MonetaryAmount value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(MonetaryAmount value) {
			return (Serializable) value;
		}

		@Override
		public MonetaryAmount assemble(Serializable cached, Object owner) {
			return (MonetaryAmount) cached;
		}

		@Override
		public MonetaryAmount replace(MonetaryAmount detached, MonetaryAmount managed, Object owner) {
			return detached;
		}
	}

	@Entity(name="RecordAsCompositeTypeEmbeddableEntity")
	public static class RecordAsCompositeTypeEmbeddableEntity {
		@Id
		Long id;

		@Embedded
		@AttributeOverride(name = "currency", column = @Column(name = "CURRENCY"))
		@AttributeOverride(name = "amount", column = @Column(name = "AMOUNT"))
		@CompositeType(MonetaryAmountType.class)
		MonetaryAmount amount;

		public RecordAsCompositeTypeEmbeddableEntity() {
		}


		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public MonetaryAmount getAmount() {
			return amount;
		}

		public void setAmount(MonetaryAmount amount) {
			this.amount = amount;
		}
	}
}
