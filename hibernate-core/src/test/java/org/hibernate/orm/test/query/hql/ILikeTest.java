/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
public class ILikeTest {

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
				Query q = session.createQuery( "from BasicEntity be where be.data like 'Prod%'" );
				List l = q.getResultList();
				assertEquals( 5, l.size() );
			}
		);
	}

	@Test
	public void testNotLike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data not like 'Prod%'" );
					List l = q.getResultList();
					assertEquals( 4, l.size() );
				}
		);
	}

	@Test
	public void testLikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data like 'Pr%$_%' escape '$'" );
					List l = q.getResultList();
					assertEquals( 2, l.size() );
				}
		);
	}

	@Test
	public void testLikeEscapeParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data like 'Pr%$_%' escape :esc" )
							.setParameter("esc", '$');
					List l = q.getResultList();
					assertEquals( 2, l.size() );
				}
		);
	}

	@Test
	public void testNotLikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data not like 'Pr%$_%' escape '$'" );
					List l = q.getResultList();
					assertEquals( 7, l.size() );
				}
		);
	}

	@Test
	public void testIlike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data ilike 'Produ%'" );
					List l = q.getResultList();
					assertEquals( 5, l.size() );
				}
		);
	}

	@Test
	public void testNotIlike(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data not ilike 'Produ%'" );
					List l = q.getResultList();
					assertEquals( 4, l.size() );
				}
		);
	}

	@Test
	public void testIlikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data ilike 'Pr%$_%' escape '$'" );
					List l = q.getResultList();
					assertEquals( 3, l.size() );
				}
		);
	}

	@Test
	public void testNotIlikeEscape(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from BasicEntity be where be.data not ilike 'Pr%$_%' escape '$'" );
					List l = q.getResultList();
					assertEquals( 6, l.size() );
				}
		);
	}
}
