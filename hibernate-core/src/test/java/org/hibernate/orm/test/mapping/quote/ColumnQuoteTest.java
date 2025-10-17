/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@DomainModel(
		annotatedClasses = {
				ColumnQuoteTest.Product.class
		}
)
@SessionFactory
@ServiceRegistry
@SuppressWarnings( "unused" )
public class ColumnQuoteTest {

	private Long testProductId = 1L;

	private String fieldWithHibernateQuotingValue = "value1";
	private String fieldWithJpaQuotingValue = "value2";

	private String changedFieldWithHibernateQuotingValue = "changedValue1";
	private String changedFieldWithJpaQuotingValue = "changedValue2";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = new Product( testProductId, fieldWithHibernateQuotingValue, fieldWithJpaQuotingValue );
			session.persist( product );
		} );
		scope.getSessionFactory().getQueryEngine().getInterpretationCache().close();
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = session.find( Product.class, testProductId );
			assertThat( product.fieldWithHibernateQuoating, is( fieldWithHibernateQuotingValue ) );
			assertThat( product.fieldWithJpaQuoting, is( fieldWithJpaQuotingValue ) );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = session.find( Product.class, testProductId );
			product.fieldWithHibernateQuoating = changedFieldWithHibernateQuotingValue;
			product.fieldWithJpaQuoting = changedFieldWithJpaQuotingValue;
			session.flush();
		} );
		scope.inTransaction( session -> {
			Product product = session.find( Product.class, testProductId );
			assertThat( product.fieldWithHibernateQuoating, is( changedFieldWithHibernateQuotingValue ) );
			assertThat( product.fieldWithJpaQuoting, is( changedFieldWithJpaQuotingValue ) );
		} );
	}

	@Entity(name = "Product")
	@Table(name = "Product")
	public static class Product {
		@Id
		private Long id;

		@Column(name = "`field1`") // Hibernate quoting
		private String fieldWithHibernateQuoating;

		@Column(name = "\"field2\"") // JPA quoting
		private String fieldWithJpaQuoting;

		public Product() {
		}

		public Product(Long id, String fieldWithHibernateQuoating, String fieldWithJpaQuoting) {
			this.id = id;
			this.fieldWithHibernateQuoating = fieldWithHibernateQuoating;
			this.fieldWithJpaQuoting = fieldWithJpaQuoting;
		}

	}
}
