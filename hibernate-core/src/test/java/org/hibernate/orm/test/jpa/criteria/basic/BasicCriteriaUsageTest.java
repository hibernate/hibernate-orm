/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Steve Ebersole
 */
@Jpa( annotatedClasses = { Wall.class, Payment.class } )
public class BasicCriteriaUsageTest {

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createQuery( "delete Wall" ).executeUpdate();
			em.createQuery( "delete Payment" ).executeUpdate();
		} );
	}

	@Test
	void testParameterCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
			Root<Wall> from = criteria.from( Wall.class );
			ParameterExpression<String> param = em.getCriteriaBuilder().parameter( String.class );
			SingularAttribute<? super Wall, ?> colorAttribute = em.getMetamodel().entity( Wall.class ).getDeclaredSingularAttribute( "color" );
			assertThat( "metamodel returned null singular attribute", colorAttribute, notNullValue() );
			Predicate predicate = em.getCriteriaBuilder().equal( from.get( colorAttribute ), param );
			criteria.where( predicate );
			assertThat( criteria.getParameters(), hasSize( 1 ) );
		} );
	}

	@Test
	void testTrivialCompilation(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<Wall> criteria = em.getCriteriaBuilder().createQuery( Wall.class );
			criteria.from( Wall.class );
			em.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8373")
	void testFunctionCriteria(EntityManagerFactoryScope scope) {
		Wall wall = new Wall();
		wall.setColor( "yellow" );

		scope.inTransaction( em -> {
			em.persist( wall );

			CriteriaBuilder cb = em.getCriteriaBuilder();

			CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
			Root<Wall> root = query.from( Wall.class );

			query.select( root ).where( cb.equal( root.get( "color" ), cb.lower( cb.literal( "YELLOW" ) ) ) );

			Wall resultItem = em.createQuery( query ).getSingleResult();
			assertThat( resultItem, notNullValue() );
		} );
	}
    
	@Test
	@TestForIssue( jiraKey = "HHH-8914" )
	void testDoubleNegation(EntityManagerFactoryScope scope) {
		Wall wall1 = new Wall();
		wall1.setColor( "yellow" );
		Wall wall2 = new Wall();
		wall2.setColor( null );
		
		scope.inTransaction( em -> {
			em.persist( wall1 );
			em.persist( wall2 );
		} );

		scope.inTransaction( em -> {
			// Although the examples are simplified and the usages appear pointless,
			// double negatives can occur in some dynamic applications (regardless
			// if it results from bad design or not).  Ensure we handle them as expected.

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
			Root<Wall> root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.isNotNull( root.get( "color" ) ) ) );
			Wall result = em.createQuery( query ).getSingleResult();
			assertThat( result, notNullValue() );
			assertThat( result.getColor(), nullValue() );

			query = cb.createQuery( Wall.class );
			root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.not(
									cb.isNull( root.get( "color" ) ) ) ) );
			result = em.createQuery( query ).getSingleResult();
			assertThat( result, notNullValue() );
			assertThat( result.getColor(), nullValue() );

			query = cb.createQuery( Wall.class );
			root = query.from( Wall.class );
			query.select( root ).where(
					cb.not(
							cb.not(
									cb.isNotNull( root.get( "color" ) ) ) ) );
			result = em.createQuery( query ).getSingleResult();
			assertThat( result, notNullValue() );
			assertThat( result.getColor(), is( "yellow" ) );
		} );
	}

}
