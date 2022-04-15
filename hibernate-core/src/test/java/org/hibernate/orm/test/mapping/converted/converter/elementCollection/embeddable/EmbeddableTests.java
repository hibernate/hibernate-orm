/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection.embeddable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Nathan Xu
 * @author Jeff Chee Yik Seng
 */

@DomainModel(annotatedClasses = {
		EmbeddableTests.ProductEntity.class,
		EmbeddableTests.MyBigDecimalConverter.class
})
@SessionFactory
@TestForIssue(jiraKey = "HHH-15211")
class EmbeddableTests {

	@Test
	void testNoClassCastExceptionThrown(SessionFactoryScope scope) {
		final ProductEntity entity = new ProductEntity();
		entity.prices = Collections.singletonList( new ProductPrice( new MyBigDecimal( 100.0 ) ) );
		final Integer productId = scope.fromTransaction( session -> {
			session.persist( entity );
			return entity.productId;
		} );

		// without fixing, the following statement would thrown "ClassCastException"
		scope.inTransaction( session -> session.get(
				ProductEntity.class,
				productId
		) );
	}

	@Entity(name = "ProductEntity")
	static class ProductEntity {
		@Id
		@GeneratedValue
		Integer productId;

		@ElementCollection(fetch = FetchType.EAGER)
		List<ProductPrice> prices = new ArrayList<>();
	}

	@Embeddable
	static class ProductPrice {
		MyBigDecimal price;

		ProductPrice() {}
		ProductPrice(MyBigDecimal price) {
			this.price = price;
		}
	}

	static class MyBigDecimal {
		double value;

		MyBigDecimal(double value) {
			this.value = value;
		}
	}

	@Converter(autoApply = true)
	static class MyBigDecimalConverter implements AttributeConverter<MyBigDecimal, BigDecimal> {
		@Override
		public BigDecimal convertToDatabaseColumn(MyBigDecimal attribute) {
			return BigDecimal.valueOf( attribute.value );
		}

		@Override
		public MyBigDecimal convertToEntityAttribute(BigDecimal dbData) {
			return new MyBigDecimal( dbData.doubleValue() );
		}

		public MyBigDecimalConverter() {
			System.out.println( "Registered MyBigDecimalConverter" );
		}
	}

}
