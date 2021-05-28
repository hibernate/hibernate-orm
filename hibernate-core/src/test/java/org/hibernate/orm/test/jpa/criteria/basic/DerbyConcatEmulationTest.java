package org.hibernate.orm.test.jpa.criteria.basic;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.orm.test.jpa.criteria.AbstractCriteriaTest;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 */
@RequiresDialect( DerbyDialect.class )
public class DerbyConcatEmulationTest extends AbstractCriteriaTest {

	@Test
	void testConcatWithAllLiteralArgs(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = cb.createQuery( Product.class );
			Root<Product> root = criteria.from( Product.class );

			criteria.select( root ).where( cb.equal( root.get( "name" ), cb.concat( cb.literal( "prefix" ), cb.literal( "suffix" ) ) ) );

			em.createQuery( criteria ).getResultList();
		} );
	}

	@Test
	void testConcatWithAllParameterArgs(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = cb.createQuery( Product.class );
			Root<Product> root = criteria.from( Product.class );

			ParameterExpression<String> prefixParam = cb.parameter( String.class );
			ParameterExpression<String> suffixParam = cb.parameter( String.class );
			criteria.select( root ).where( cb.equal( root.get( "name" ), cb.concat( prefixParam, suffixParam ) ) );

			TypedQuery<Product> query = em.createQuery( criteria );
			query.setParameter( prefixParam, "prefix" ).setParameter( suffixParam, "suffix" ).getResultList();
		} );
	}

	@Test
	void testConcatWithMixedTypeArgs(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Product> criteria = cb.createQuery( Product.class );
			Root<Product> root = criteria.from( Product.class );

			ParameterExpression<String> prefixParam = cb.parameter( String.class );
			criteria.select( root ).where( cb.equal( root.get( "name" ), cb.concat( prefixParam, cb.literal( "suffix" ) ) ) );
			TypedQuery<Product> query = em.createQuery( criteria );
			query.setParameter( prefixParam, "prefix" ).getResultList();
		} );
	}

}