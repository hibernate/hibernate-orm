/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.common.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class BasicCriteriaExecutionTests {

	@Test
	public void testExecutingBasicCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );

					session.createQuery( criteria ).list();
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<BasicEntity> criteria = criteriaBuilder.createQuery( BasicEntity.class );
					criteria.from( BasicEntity.class );
					session.createQuery( criteria ).list();
				}
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryInStatelessSession(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );

					session.createQuery( criteria ).list();
				}
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryLiteralPredicate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );
					criteria.where(
							criteriaBuilder.equal(
									criteriaBuilder.literal( 1 ),
									criteriaBuilder.literal( 1 )
							)
					);

					session.createQuery( criteria ).list();
				}
		);
	}

	@Test
	public void testExecutingBasicCriteriaQueryLiteralPredicateInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );
					criteria.where(
							criteriaBuilder.equal(
									criteriaBuilder.literal( 1 ),
									criteriaBuilder.literal( 1 )
							)
					);
					session.createQuery( criteria ).list();
				}
		);
	}

	// Doing ... where ? = ? ... is only allowed in a few DBs. Since this is useless, we don't bother to emulate this
	@Test
	@RequiresDialect(H2Dialect.class)
	@SkipForDialect(value = DerbyDialect.class, comment = "Derby doesn't support comparing parameters against each other")
	public void testExecutingBasicCriteriaQueryParameterPredicate(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );
					final ParameterExpression<Integer> param = criteriaBuilder.parameter( Integer.class );
					criteria.where( criteriaBuilder.equal( param, param ) );

					session.createQuery( criteria ).setParameter( param, 1 ).list();
				}
		);
	}

	// Doing ... where ? = ? ... is only allowed in a few DBs. Since this is useless, we don't bother to emulate this
	@Test
	@RequiresDialect(H2Dialect.class)
	@SkipForDialect(value = DerbyDialect.class, comment = "Derby doesn't support comparing parameters against each other")
	public void testExecutingBasicCriteriaQueryParameterPredicateInStatelessSession(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final CriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final Root<BasicEntity> root = criteria.from( BasicEntity.class );
					criteria.select( root );
					final ParameterExpression<Integer> param = criteriaBuilder.parameter( Integer.class );
					criteria.where( criteriaBuilder.equal( param, param ) );

					session.createQuery( criteria ).setParameter( param, 1 ).list();
				}
		);
	}

	@Test
	public void testCriteriaEntityJoin(SessionFactoryScope scope) {
		scope.inStatelessTransaction(
				session -> {
					final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

					final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();
					final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );
					root.join( BasicEntity.class, JoinType.CROSS );
					criteria.select( root );

					session.createQuery( criteria ).list();
				}
		);
	}
}
