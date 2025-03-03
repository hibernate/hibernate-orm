/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.List;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@JiraKey(value = "HHH-13169")
public class UpdateJoinedSubclassCorrelationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Root.class, SubRoot.class, Detail.class };
	}

	@Test
	public void testJoinedSubclassUpdateWithCorrelation() {
		// prepare
		doInJPA( this::entityManagerFactory, entityManager -> {
			Root m1 = new SubRoot( 1, null );
			entityManager.persist( m1 );
			Detail d11 = new Detail( 10, m1 );
			entityManager.persist( d11 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			// DO NOT CHANGE this query: it used to trigger a very specific bug caused
			// by the root table alias being added to the generated subquery instead of the table name
			String u = "update SubRoot m set name = (select 'test' from Detail d where d.root = m)";
			Query updateQuery = entityManager.createQuery( u );
			updateQuery.executeUpdate();

			// so check if the name of the SubRoot has been correctly updated
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Root> query = builder.createQuery( Root.class );
			query.select( query.from( Root.class ) );
			List<Root> roots = entityManager.createQuery( query ).getResultList();
			Assert.assertEquals( 1, roots.size() );
			Assert.assertEquals( "test", ((SubRoot) roots.get(0)).name );
		} );
	}

	@Inheritance(strategy = JOINED)
	@Entity(name = "Root")
	public static abstract class Root {
		@Id
		private Integer id;

		public Root() { }

		public Root(Integer id ) {
			this.id = id;
		}
	}

	@Entity(name = "SubRoot")
	public static class SubRoot extends Root {
		private String name;

		public SubRoot() { }

		public SubRoot(Integer id, String name ) {
			super(id);
			this.name = name;
		}
	}

	@Entity(name = "Detail")
	public static class Detail {
		@Id
		private Integer id;

		@ManyToOne(optional = false)
		private Root root;

		public Detail( Integer id, Root root) {
			this.id = id;
			this.root = root;
		}
	}
}
