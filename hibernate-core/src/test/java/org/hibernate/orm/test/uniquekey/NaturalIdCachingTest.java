/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.uniquekey;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.NaturalId;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		NaturalIdCachingTest.PropertyHolder.class,
		NaturalIdCachingTest.Property.class
})
@SessionFactory(generateStatistics = true)
public class NaturalIdCachingTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		final StatisticsImplementor statistics = factoryScope.getSessionFactory().getStatistics();
		assertTrue( statistics.isStatisticsEnabled() );

		factoryScope.inTransaction( session -> {
			Property property = new Property( 1, 1, 1 );
			session.persist( property );
			session.persist( new PropertyHolder( 1, property ) );
			session.persist( new PropertyHolder( 2, property ) );
		} );

		assertEquals( 3L, statistics.getEntityInsertCount() );
		statistics.clear();

		factoryScope.inTransaction( session -> {
			session.find( PropertyHolder.class, 1 );
			session.find( PropertyHolder.class, 2 );
		} );

		assertEquals( 3L, statistics.getEntityLoadCount() );
		assertEquals( 3L, statistics.getPrepareStatementCount() );
	}

	@Entity(name = "PropertyHolder")
	public static class PropertyHolder implements Serializable {

		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="PROP_CODE", referencedColumnName = "CODE")
		@JoinColumn(name="PROP_ITEM", referencedColumnName = "ITEM")
		private Property property;

		private String severalOtherFields = "Several other fields ...";

		protected PropertyHolder() {}

		public PropertyHolder(Integer id, Property property) {
			this.id = id;
			this.property = property;
		}

	}

	@Entity(name = "PropertyEntity")
	public static class Property implements Serializable {

		@Id
		private Integer id;

		@NaturalId
		private Integer code;

		@NaturalId
		private Integer item;

		private String description = "A description ...";

		protected Property(){}

		public Property(Integer id, Integer code, Integer item) {
			this.id = id;
			this.code = code;
			this.item = item;
		}
	}
}
