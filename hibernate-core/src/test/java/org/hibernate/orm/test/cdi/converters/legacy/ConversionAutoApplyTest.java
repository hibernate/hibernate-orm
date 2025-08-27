/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.converters.legacy;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

/**
 * Test AttributeConverter functioning in case where entity type is derived from jdbc type.
 *
 * @author Karthik Abram
 */
@JiraKey( value = "HHH-10549" )
public class ConversionAutoApplyTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testConverterIsNotIncorrectlyApplied() {
		Widget w = new Widget();
		w.setId( 1 );
		w.setDimension( new BigDecimal( "1.0" ) );
		w.setCost( new Money( "2.0" ) );

		EntityManager em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( w );
		em.getTransaction().commit();
		em.close();

		em = entityManagerFactory().createEntityManager();
		em.getTransaction().begin();
		Widget recorded = em.find( Widget.class, 1 );
		assertEquals( 1, recorded.getId() );

		em.remove(recorded);
		em.getTransaction().commit();
		em.close();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Widget.class, MoneyConverter.class };
	}


	public static class Money extends BigDecimal {

		public Money(String value) {
			super( value );
		}


		public Money(BigDecimal value) {
			super( value.toString() );
		}


		@Override
		public BigDecimal add(BigDecimal augend) {
			return new Money( this.add( augend ).setScale( 10, RoundingMode.HALF_EVEN ) );
		}
	}


	@Converter( autoApply = true )
	public static class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

		@Override
		public BigDecimal convertToDatabaseColumn(Money attribute) {
			return attribute == null ? null : new BigDecimal(attribute.toString());
		}


		@Override
		public Money convertToEntityAttribute(BigDecimal dbData) {
			return dbData == null ? null : new Money( dbData.toString() );
		}

	}


	@Entity
	@Table( name = "Widget" )
	public static class Widget {

		private int id;
		private BigDecimal dimension;
		private Money cost;


		@Id
		public int getId() {
			return id;
		}


		public void setId(int id) {
			this.id = id;
		}


		public BigDecimal getDimension() {
			return dimension;
		}


		public void setDimension(BigDecimal dimension) {
			this.dimension = dimension;
		}


		@Column( name = "cost" )
		public Money getCost() {
			return cost;
		}


		public void setCost(Money cost) {
			this.cost = cost;
		}

	}





}
