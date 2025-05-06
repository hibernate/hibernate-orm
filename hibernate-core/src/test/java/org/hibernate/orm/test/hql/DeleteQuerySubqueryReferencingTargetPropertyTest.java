/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@JiraKey(value = "HHH-12492")
public class DeleteQuerySubqueryReferencingTargetPropertyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Root.class, Detail.class };
	}

	@Test
	public void testSubQueryReferencingTargetProperty() {
		// prepare
		doInJPA( this::entityManagerFactory, entityManager -> {
			Root m1 = new Root();
			entityManager.persist( m1 );
			Detail d11 = new Detail( m1 );
			entityManager.persist( d11 );
			Detail d12 = new Detail( m1 );
			entityManager.persist( d12 );

			Root m2 = new Root();
			entityManager.persist( m2 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			// depending on the generated ids above this delete removes all Roots or nothing
			// removal of all Roots results in foreign key constraint violation
			// removal of nothing is incorrect since 2nd Root does not have any details

			// DO NOT CHANGE this query: it used to trigger a very specific bug caused
			// by the alias not being added to the generated query
			String d = "delete from Root m where not exists (select d from Detail d where d.root=m)";
			Query del = entityManager.createQuery( d );
			del.executeUpdate();

			// so check for exactly one Root after deletion
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Root> query = builder.createQuery( Root.class );
			query.select( query.from( Root.class ) );
			Assert.assertEquals( 1, entityManager.createQuery( query ).getResultList().size() );
		} );
	}

	@Entity(name = "Root")
	public static class Root {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity(name = "Detail")
	public static class Detail {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne(optional = false)
		private Root root;

		public Detail(Root root) {
			this.root = root;
		}
	}
}
