/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

/**
 * @author Will Dazy
 */
@Jpa(annotatedClasses = CoalesceTest.HHH15291Entity.class)
@JiraKey( value = "HHH-15291")
public class CoalesceTest {

	@Test
	public void hhh15291JPQL1Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					TypedQuery<HHH15291Entity> query = entityManager.createQuery(
							"" + "SELECT t FROM HHH15291Entity t "
									+ "WHERE t.itemString2 = " + "COALESCE (t.itemString1, ?1)",
							HHH15291Entity.class
					);
					query.setParameter( 1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Test
	public void hhh15291JPQL2Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					TypedQuery<String> query2 = entityManager.createQuery(
							"" + "SELECT COALESCE (t.itemString2, ?1) FROM HHH15291Entity t ORDER BY t.itemInteger1 ASC",
							String.class
					);
					query2.setParameter( 1, "Sample" );
				}
		);
	}

	@Test
	public void hhh15291Criteria1Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<HHH15291Entity> cquery = cb.createQuery( HHH15291Entity.class );
					Root<HHH15291Entity> root = cquery.from( HHH15291Entity.class );
					cquery.select( root );

					ParameterExpression<String> checkParam1 = cb.parameter( String.class );
					Expression<String> coalesce = cb.coalesce( root.get( "itemString1" ), checkParam1 );
					cquery.where( cb.equal( root.get( "itemString2" ), coalesce ) );

					TypedQuery<HHH15291Entity> query = entityManager.createQuery( cquery );
					query.setParameter( checkParam1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Test
	public void hhh15291Criteria2Test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<HHH15291Entity> cquery = cb.createQuery( HHH15291Entity.class );
					Root<HHH15291Entity> root = cquery.from( HHH15291Entity.class );
					cquery.select( root );

					ParameterExpression<String> checkParam1 = cb.parameter( String.class );
					Expression<String> coalesce = cb.coalesce( root.get( "itemString1" ), checkParam1 );
					cquery.where( cb.equal( root.get( "itemString2" ), coalesce ) );

					TypedQuery<HHH15291Entity> query = entityManager.createQuery( cquery );
					query.setParameter( checkParam1, "Sample" );
					query.getResultList();
				}
		);
	}

	@Entity(name = "HHH15291Entity")
	public static class HHH15291Entity {

		@Id
		@Column(name = "KEY_CHAR")
		private String KeyString;

		@Column(name = "ITEM_STRING1")
		private String itemString1;

		@Column(name = "ITEM_STRING2")
		private String itemString2;

		@Column(name = "ITEM_STRING3")
		private String itemString3;

		@Column(name = "ITEM_STRING4")
		private String itemString4;

		@Column(name = "ITEM_INTEGER1")
		private Integer itemInteger1;

		public String getKeyString() {
			return KeyString;
		}

		public void setKeyString(String keyString) {
			KeyString = keyString;
		}

		public String getItemString1() {
			return itemString1;
		}

		public void setItemString1(String itemString1) {
			this.itemString1 = itemString1;
		}

		public String getItemString2() {
			return itemString2;
		}

		public void setItemString2(String itemString2) {
			this.itemString2 = itemString2;
		}

		public String getItemString3() {
			return itemString3;
		}

		public void setItemString3(String itemString3) {
			this.itemString3 = itemString3;
		}

		public String getItemString4() {
			return itemString4;
		}

		public void setItemString4(String itemString4) {
			this.itemString4 = itemString4;
		}

		public Integer getItemInteger1() {
			return itemInteger1;
		}

		public void setItemInteger1(Integer itemInteger1) {
			this.itemInteger1 = itemInteger1;
		}
	}
}
