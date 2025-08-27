/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
public class LikeEscapeDefaultTest {
	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					BasicEntity be1 = new BasicEntity( 1, "Product\\one" );
					em.persist( be1 );
					BasicEntity be2 = new BasicEntity( 2, "Product%two" );
					em.persist( be2 );
					BasicEntity be3 = new BasicEntity( 3, "Product\"three" );
					em.persist( be3 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not support empty escape ''")
	public void testDefaultEscapeBackslash(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<BasicEntity> q = session.createQuery(
					"from BasicEntity be where be.data like ?1",
					BasicEntity.class
			).setParameter( 1, "%\\%" );
			List<BasicEntity> l = q.getResultList();
			assertEquals( 1, l.size() );
			assertEquals( 1, l.get( 0 ).getId() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not support empty escape ''")
	public void testDefaultEscapeBackslashLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<BasicEntity> q = session.createQuery(
					"from BasicEntity be where be.data like '%\\%'",
					BasicEntity.class
			);
			List<BasicEntity> l = q.getResultList();
			assertEquals( 1, l.size() );
			assertEquals( 1, l.get( 0 ).getId() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not support empty escape ''")
	public void testDefaultEscapeNoResults(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<BasicEntity> q = session.createQuery(
					"from BasicEntity be where be.data like ?1",
					BasicEntity.class
			).setParameter( 1, "%\\\"%" );
			List<BasicEntity> l = q.getResultList();
			assertEquals( 0, l.size() );
		} );
	}

	@Test
	public void testExplicitEscapeLiteralBackslash(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<BasicEntity> q = session.createQuery(
					"from BasicEntity be where be.data like ?1 escape '\\'",
					BasicEntity.class
			).setParameter( 1, "%\\%%" );
			List<BasicEntity> l = q.getResultList();
			assertEquals( 1, l.size() );
			assertEquals( 2, l.get( 0 ).getId() );
		} );
	}

	@Test
	public void testExplicitEscapeLiteralOtherChar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<BasicEntity> q = session.createQuery(
					"from BasicEntity be where be.data like ?1 escape '#'",
					BasicEntity.class
			).setParameter( 1, "%#%%" );
			List<BasicEntity> l = q.getResultList();
			assertEquals( 1, l.size() );
			assertEquals( 2, l.get( 0 ).getId() );
		} );
	}
}
