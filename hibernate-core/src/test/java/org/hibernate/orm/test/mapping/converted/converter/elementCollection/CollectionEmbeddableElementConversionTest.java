/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.elementCollection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;

/**
 * @author Nathan Xu
 * @author Jeff Chee Yik Seng
 */

@DomainModel(annotatedClasses = {
		CollectionEmbeddableElementConversionTest.ProductEntity.class,
		CollectionEmbeddableElementConversionTest.MyBigDecimalConverter.class
})
@SessionFactory
@JiraKey(value = "HHH-15211")
class CollectionEmbeddableElementConversionTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		final ProductEntity entity = new ProductEntity( 1 );
		entity.prices = Collections.singletonList( new ProductPrice( new MyBigDecimal( 100.0 ) ) );
		scope.fromTransaction( session -> {
			session.persist( entity );
			return entity.productId;
		} );
	}

	@Test
	void testNoClassCastExceptionThrown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.get(
				ProductEntity.class,
				1
		) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "ProductEntity")
	static class ProductEntity {
		@Id
		Integer productId;

		ProductEntity() {
		}

		ProductEntity(Integer productId) {
			this.productId = productId;
		}

		@ElementCollection(fetch = FetchType.EAGER)
		List<ProductPrice> prices = new ArrayList<>();
	}

	@Embeddable
	static class ProductPrice {
		MyBigDecimal price;

		ProductPrice() {
		}

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

	}

}
