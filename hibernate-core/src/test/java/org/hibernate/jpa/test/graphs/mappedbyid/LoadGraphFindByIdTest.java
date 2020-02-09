/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.mappedbyid;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
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
	@TestForIssue(jiraKey = "HHH-10842")
	@FailureExpected( jiraKey = "HHH-10842" )
	public void findByPrimaryKeyWithId() {
		doInJPA( this::entityManagerFactory, em -> {
			User result = em.find( User.class, 1L, createProperties( em ) );
			Assert.assertNotNull( result.userStatistic.commentCount );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10842")
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
