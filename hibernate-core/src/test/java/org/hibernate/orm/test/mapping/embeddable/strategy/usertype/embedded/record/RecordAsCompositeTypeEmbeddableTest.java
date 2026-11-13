package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.record;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.annotations.CompositeType;
import org.hibernate.metamodel.spi.ValueAccess;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.usertype.CompositeUserType;

import org.javamoney.moneta.FastMoney;

import org.junit.jupiter.api.Test;

import javax.money.MonetaryAmount;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = RecordAsCompositeTypeEmbeddableTest.RecordAsCompositeTypeEmbeddableEntity.class)
@SessionFactory
public class RecordAsCompositeTypeEmbeddableTest {

	@Test
	public void test(SessionFactoryScope scope) {
		MonetaryAmount amount = FastMoney.of( 1, "BRL" );

		RecordAsCompositeTypeEmbeddableEntity entity = new RecordAsCompositeTypeEmbeddableEntity();
		entity.setAmount( amount );
		entity.setId( 1L );

		scope.inTransaction( session -> session.persist(entity) );

		scope.inTransaction( session -> {
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

		public MonetaryAmountType() {
		}

		@Override
		@Nullable
		public Object getPropertyValue(@Nonnull MonetaryAmount component, int property) throws HibernateException {
			//Alphabetical
			return switch ( property ) {
				case 0 -> component.getNumber().numberValueExact( BigDecimal.class );
				case 1 -> component.getCurrency().getCurrencyCode();
				default -> null;
			};
		}

		@Override
		@Nonnull
		public MonetaryAmount instantiate(@Nonnull ValueAccess values) {
			//Alphabetical
			BigDecimal amount = values.getValue( 0, BigDecimal.class );
			String currency = values.getValue( 1, String.class );
			return FastMoney.of( amount, currency );
		}

		@Override
		@Nonnull
		public Class<?> embeddable() {
			return MonetaryAmountMapper.class;
		}

		@Override
		@Nonnull
		public Class<MonetaryAmount> returnedClass() {
			return MonetaryAmount.class;
		}

		@Override
		public boolean equals(@Nullable MonetaryAmount x, @Nullable MonetaryAmount y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(@Nonnull MonetaryAmount x) {
			return x.hashCode();
		}

		@Override
		@Nonnull
		public MonetaryAmount deepCopy(@Nonnull MonetaryAmount value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		@Nonnull
		public Serializable disassemble(@Nonnull MonetaryAmount value) {
			return (Serializable) value;
		}

		@Override
		@Nonnull
		public MonetaryAmount assemble(@Nonnull Serializable cached, @Nullable Object owner) {
			return (MonetaryAmount) cached;
		}

		@Override
		@Nonnull
		public MonetaryAmount replace(
				@Nonnull MonetaryAmount detached,
				@Nullable MonetaryAmount managed,
				@Nullable Object owner) {
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
