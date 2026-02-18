/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"))
public class ILikeCriteriaTest {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					BasicEntity be1 = new BasicEntity(1, "Product_one");
					em.persist( be1 );
					BasicEntity be2 = new BasicEntity(2, "proDUct two");
					em.persist( be2 );
					BasicEntity be3 = new BasicEntity(3, "Product three");
					em.persist( be3 );
					BasicEntity be4 = new BasicEntity(4, "pROducT four");
					em.persist( be4 );
					BasicEntity be5 = new BasicEntity(5, "Product five");
					em.persist( be5 );
					BasicEntity be6 = new BasicEntity(6, "Prodact six");
					em.persist( be6 );
					BasicEntity be7 = new BasicEntity(7, "prodACt seven");
					em.persist( be7 );
					BasicEntity be8 = new BasicEntity(8, "Prod_act eight");
					em.persist( be8 );
					BasicEntity be9 = new BasicEntity(9, "prod_ACt nine");
					em.persist( be9 );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testLike(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {
				HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
				CriteriaQuery cq = cb.createQuery( BasicEntity.class);
				Root<BasicEntity> from = cq.from( BasicEntity.class );

				cq.where( cb.like( from.get( "data" ), "Prod%" ) );
				Query q = session.createQuery( cq );
				List l = q.getResultList();
				assertEquals( 5, l.size() );

				cq.where( cb.like( from.get( "data" ), cb.literal( "Prod%" ) ) );
				q = session.createQuery( cq );
				l = q.getResultList();
				assertEquals( 5, l.size() );
			}
		);
	}

	@Test
	public void testLikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.like( from.get( "data" ), cb.literal( "Prod%" ), cb.literal( '$' ) ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 5, l.size() );

					cq.where( cb.like( from.get( "data" ), cb.literal( "Prod%" ), '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 5, l.size() );

					cq.where( cb.like( from.get( "data" ), "Prod%", cb.literal( '$' ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 5, l.size() );

					cq.where( cb.like( from.get( "data" ), "Prod%", '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 5, l.size() );
				}
		);
	}

	@Test
	public void testNotLike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.notLike( from.get( "data" ), "Prod%" ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 4, l.size() );

					cq.where( cb.notLike( from.get( "data" ), cb.literal( "Prod%" ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 4, l.size() );
				}
		);
	}

	@Test
	public void testNotLikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.notLike( from.get( "data" ), cb.literal( "Pr%$_%" ), cb.literal( '$' ) ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 7, l.size() );

					cq.where( cb.notLike( from.get( "data" ), cb.literal( "Pr%$_%" ), '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 7, l.size() );

					cq.where( cb.notLike( from.get( "data" ), "Pr%$_%", cb.literal( '$' ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 7, l.size() );

					cq.where( cb.notLike( from.get( "data" ), "Pr%$_%", '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 7, l.size() );
				}
		);
	}

	@Test
	public void testIlike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.ilike( from.get( "data" ), "Produ%" ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 5, l.size() );

					cq.where( cb.ilike( from.get( "data" ), cb.literal( "Produ%" ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 5, l.size() );
				}
		);
	}

	@Test
	public void testIlikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.ilike( from.get( "data" ), cb.literal( "Pr%$_%" ), cb.literal( '$' ) ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 3, l.size() );

					cq.where( cb.ilike( from.get( "data" ), cb.literal( "Pr%$_%" ), '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 3, l.size() );

					cq.where( cb.ilike( from.get( "data" ), "Pr%$_%", cb.literal( '$' ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 3, l.size() );

					cq.where( cb.ilike( from.get( "data" ), "Pr%$_%", '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 3, l.size() );
				}
		);
	}

	@Test
	public void testNotIlike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.notIlike( from.get( "data" ), "Produ%" ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 4, l.size() );

					cq.where( cb.notIlike( from.get( "data" ), cb.literal( "Produ%" ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 4, l.size() );
				}
		);
	}

	@Test
	public void testNotIlikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery cq = cb.createQuery( BasicEntity.class);
					Root<BasicEntity> from = cq.from( BasicEntity.class );

					cq.where( cb.notIlike( from.get( "data" ), cb.literal( "Pr%$_%" ), cb.literal( '$' ) ) );
					Query q = session.createQuery( cq );
					List l = q.getResultList();
					assertEquals( 6, l.size() );

					cq.where( cb.notIlike( from.get( "data" ), cb.literal( "Pr%$_%" ), '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 6, l.size() );

					cq.where( cb.notIlike( from.get( "data" ), "Pr%$_%", cb.literal( '$' ) ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 6, l.size() );

					cq.where( cb.notIlike( from.get( "data" ), "Pr%$_%", '$' ) );
					q = session.createQuery( cq );
					l = q.getResultList();
					assertEquals( 6, l.size() );
				}
		);
	}
}
