/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.mixed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.AnyDiscriminatorValueStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class LegacyMixedStrategyTests {
	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, Order.class, AnotherPaymentType.class})
	@SessionFactory
	void testHandling(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Order order = new Order( 1, "1" );
			final CashPayment cashPayment = new CashPayment( 1, 50.00 );
			final CardPayment cardPayment = new CardPayment( 1, 150.00, "123-456-789" );
			final AnotherPaymentType anotherPayment = new AnotherPaymentType( 1, 250.00 );
			session.persist( order );
			session.persist( cashPayment );
			session.persist( cardPayment );
			session.persist( anotherPayment );
			session.flush();

			order.setPayment( cashPayment );
			session.flush();

			// this one will fail because historically Hibernate expected either -
			// 		1. all discriminator values to be mapped
			// 		2. no discriminator values to be mapped
			// this is the crux of the work for HHH-18728
			try {
				order.setPayment( cardPayment );
				session.flush();
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown entity name" );
			}

			// this one will fail for the same reason
			try {
				order.setPayment( anotherPayment );
				session.flush();
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown entity name" );
			}
		} );
	}

	@Test
	@DomainModel(annotatedClasses = {Payment.class, CashPayment.class, CardPayment.class, Order.class, AnotherPaymentType.class})
	@SessionFactory
	void verifyDomainModel(SessionFactoryScope sessions) {
		sessions.withSessionFactory( (factory) -> {
			final EntityPersister entityDescriptor = factory.getMappingMetamodel().getEntityDescriptor( Order.class );
			final DiscriminatedAssociationAttributeMapping paymentMapping = (DiscriminatedAssociationAttributeMapping) entityDescriptor.findAttributeMapping( "payment" );
			final DiscriminatorMapping discriminatorMapping = paymentMapping.getDiscriminatorMapping();
			final DiscriminatorConverter<?, ?> discriminatorConverter = discriminatorMapping.getValueConverter();
			// historically this operated as if EXPLICIT
			assertThat( discriminatorConverter.getValueStrategy() ).isEqualTo( AnyDiscriminatorValueStrategy.EXPLICIT );

			final DiscriminatorValueDetails cash = discriminatorConverter.getDetailsForDiscriminatorValue( "CASH" );
			assertThat( cash.getIndicatedEntity().getEntityName() ).isEqualTo( CashPayment.class.getName() );
			assertThat( cash.getIndicatedEntityName() ).isEqualTo( CashPayment.class.getName() );

			try {
				discriminatorConverter.getDetailsForDiscriminatorValue( CardPayment.class.getName() );
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown discriminator value" );
			}

			try {
				discriminatorConverter.getDetailsForDiscriminatorValue( AnotherPaymentType.class.getName() );
				fail( "Expecting an error" );
			}
			catch (HibernateException expected) {
				assertThat( expected ).hasMessageContaining( "Unknown discriminator value" );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.createMutationQuery( "delete Order" ).executeUpdate();
			session.createMutationQuery( "delete CashPayment" ).executeUpdate();
			session.createMutationQuery( "delete CardPayment" ).executeUpdate();
		} );
	}

	@SuppressWarnings("unused")
	@Entity(name="AnotherPaymentType")
	@Table(name="AnotherPaymentType")
	public static class AnotherPaymentType implements Payment {
		@Id
		public Integer id;
		public Double amount;

		public AnotherPaymentType() {
		}

		public AnotherPaymentType(Integer id, Double amount) {
			this.id = id;
			this.amount = amount;
		}

		@Override
		public Double getAmount() {
			return amount;
		}
	}

	@Entity(name="Order")
	@Table(name="orders")
	public static class Order {
		@Id
		private Integer id;
		private String name;

		@Any
		@AnyKeyJavaClass( Integer.class )
		@JoinColumn(name = "payment_fk")
		@AnyDiscriminatorValue( discriminator = "CASH", entity = CashPayment.class )
		private Payment payment;

		public Order() {
		}

		public Order(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Payment getPayment() {
			return payment;
		}

		public void setPayment(Payment payment) {
			this.payment = payment;
		}
	}
}
