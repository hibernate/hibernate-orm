/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.formula;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Formula;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 * @author Nathan Xu
 */
@DomainModel( annotatedClasses = FormulaWithPartitionByTest.DisplayItem.class )
@SessionFactory
@RequiresDialectFeature( jiraKey = "HHH-10754", feature = DialectFeatureChecks.SupportPartitionBy.class )
public class FormulaWithPartitionByTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final DisplayItem displayItem20_1 = new DisplayItem();
			displayItem20_1.setId( 1 );
			displayItem20_1.setDiscountCode( "20" );
			displayItem20_1.setDiscountValue( 12.34d );

			final DisplayItem displayItem20_2 = new DisplayItem();
			displayItem20_2.setId( 2 );
			displayItem20_2.setDiscountCode( "20" );
			displayItem20_2.setDiscountValue( 15.89 );

			final DisplayItem displayItem100 = new DisplayItem();
			displayItem100.setId( 3 );
			displayItem100.setDiscountCode( "100" );
			displayItem100.setDiscountValue( 12.5 );

			session.persist( displayItem20_1 );
			session.persist( displayItem20_2 );
			session.persist( displayItem100 );
		} );
	}

	@Test
	@JiraKey( value = "HHH-10754" )
	void testFormulaAnnotationWithPartitionBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<DisplayItem> displayItems = session.createQuery( "select di from DisplayItem di order by di.id", DisplayItem.class).getResultList();

			assertNotNull( displayItems );
			assertEquals( 3, displayItems.size() );

			assertEquals( 1, displayItems.get( 0 ).getItemsByCode().intValue() );
			assertEquals( 2, displayItems.get( 1 ).getItemsByCode().intValue() );
			assertEquals( 1, displayItems.get( 2 ).getItemsByCode().intValue() );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "DisplayItem")
	public static class DisplayItem implements Serializable {

		@Id
		private Integer id;

		@Column(name = "DISCOUNT_CODE")
		private String discountCode;

		@Column(name = "DISCOUNT_VALUE")
		private Double discountValue;

		@Formula("ROW_NUMBER() OVER( PARTITION BY DISCOUNT_CODE ORDER BY SIGN(DISCOUNT_VALUE) DESC )")
		private Integer itemsByCode;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDiscountCode() {
			return discountCode;
		}

		public void setDiscountCode(String discountCode) {
			this.discountCode = discountCode;
		}

		public Integer getItemsByCode() {
			return itemsByCode;
		}

		public void setItemsByCode(Integer itemsByCode) {
			this.itemsByCode = itemsByCode;
		}

		public Double getDiscountValue() {
			return discountValue;
		}

		public void setDiscountValue(Double discountValue) {
			this.discountValue = discountValue;
		}
	}
}
