/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.mappedbyid;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Oliver Breidenbach
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoadGraphFindByIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class, UserStatistic.class};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, em -> {
			UserStatistic statistic = new UserStatistic();
			statistic.id = 1L;
			statistic.commentCount = 7;
			User user = new User();
			user.id = 1L;
			user.userStatistic = statistic;

			em.persist( statistic );
			em.persist( user );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10842")
	public void findByPrimaryKeyWithId() {
		doInJPA( this::entityManagerFactory, em -> {
			User result = em.find( User.class, 1L, createProperties( em ) );
			Assert.assertNotNull( result.userStatistic.commentCount );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10842")
	public void findByPrimaryKeyWithQuery() {
		doInJPA( this::entityManagerFactory, em -> {
			User result = createTypedQuery( em ).getSingleResult();
			Assert.assertNotNull( result.userStatistic.commentCount );
		} );
	}

	private TypedQuery<User> createTypedQuery(EntityManager em) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> cq = cb.createQuery( User.class );
		Root<User> root = cq.from( User.class );

		cq.where( cb.equal( root.get( "id" ), 1L ) );
		TypedQuery<User> tq = em.createQuery( cq );
		tq.setHint( "javax.persistence.loadgraph", createEntityGraph( em ) );
		return tq;
	}

	private Map<String, Object> createProperties(EntityManager em) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"javax.persistence.loadgraph",
				createEntityGraph( em )
		);
		return properties;
	}

	private EntityGraph<User> createEntityGraph(EntityManager em) {
		EntityGraph<User> entityGraph = em.createEntityGraph( User.class );
		entityGraph.addAttributeNodes( "userStatistic" );
		return entityGraph;
	}

	@Entity(name = "UserStatistic")
	public static class UserStatistic {

		@Id
		private Long id;

		private Integer commentCount;
	}

	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User {

		@Id
		private Long id;

		private String name;

		@OneToOne(fetch = FetchType.LAZY, optional = false)
		@MapsId
		private UserStatistic userStatistic;

	}
}
