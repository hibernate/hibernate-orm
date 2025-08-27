/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.Arrays;
import java.util.List;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = MultiTypedBasicAttributesEntity.class)
@SessionFactory
public class ParameterTest {

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Blobs are not allowed in this expression")
	public void testPrimitiveArrayParameterBinding(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = em.getCriteriaBuilder()
					.createQuery( MultiTypedBasicAttributesEntity.class );
			Root<MultiTypedBasicAttributesEntity> rootEntity = criteria.from( MultiTypedBasicAttributesEntity.class );
			Path<int[]> someIntsPath = rootEntity.get( MultiTypedBasicAttributesEntity_.someInts );
			ParameterExpression<int[]> param = em.getCriteriaBuilder().parameter( int[].class, "theInts" );
			criteria.where( em.getCriteriaBuilder().equal( someIntsPath, param ) );
			TypedQuery<MultiTypedBasicAttributesEntity> query = em.createQuery( criteria );
			query.setParameter( param, new int[] { 1,1,1 } );
			assertThat( query.getParameterValue( param.getName() ), instanceOf( int[].class) );
			query.getResultList();
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Blobs are not allowed in this expression")
	public void testNonPrimitiveArrayParameterBinding(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = em.getCriteriaBuilder()
					.createQuery( MultiTypedBasicAttributesEntity.class );
			Root<MultiTypedBasicAttributesEntity> rootEntity = criteria.from( MultiTypedBasicAttributesEntity.class );
			Path<Integer[]> thePath = rootEntity.get( MultiTypedBasicAttributesEntity_.someWrappedIntegers );
			ParameterExpression<Integer[]> param = em.getCriteriaBuilder().parameter( Integer[].class, "theIntegers" );
			criteria.where( em.getCriteriaBuilder().equal( thePath, param ) );
			TypedQuery<MultiTypedBasicAttributesEntity> query = em.createQuery( criteria );
			query.setParameter( param, new Integer[] { 1, 1, 1 } );
			assertThat( query.getParameterValue( param.getName() ), instanceOf( Integer[].class ) );
			query.getResultList();
		} );
	}

	@Test
	public void testNamedParameterMetadata(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = em.getCriteriaBuilder()
					.createQuery( MultiTypedBasicAttributesEntity.class );
			Root<MultiTypedBasicAttributesEntity> rootEntity = criteria.from( MultiTypedBasicAttributesEntity.class );

			criteria.where(
					em.getCriteriaBuilder().equal(
							rootEntity.get( MultiTypedBasicAttributesEntity_.id ),
							em.getCriteriaBuilder().parameter( Long.class, "id" )
					)
			);

			TypedQuery<MultiTypedBasicAttributesEntity> query = em.createQuery( criteria );
			Parameter<?> parameter = query.getParameter( "id" );
			assertEquals( "id", parameter.getName() );
		} );
	}

	@Test
	public void testParameterInParameterList(SessionFactoryScope scope) {
		// Yes, this test makes no semantic sense.  But the JPA TCK does it...
		// 		it causes a problem on Derby, which does not like the form "... where ? in (?,?)"
		//		Derby wants one side or the other to be CAST (I assume so it can check typing).
		scope.inTransaction( em -> {
			CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = em.getCriteriaBuilder()
					.createQuery( MultiTypedBasicAttributesEntity.class );
			criteria.from( MultiTypedBasicAttributesEntity.class );

			criteria.where(
					em.getCriteriaBuilder().in( em.getCriteriaBuilder().parameter( Long.class, "p1" ) )
							.value( em.getCriteriaBuilder().parameter( Long.class, "p2" ) )
							.value( em.getCriteriaBuilder().parameter( Long.class, "p3" ) )
			);

			TypedQuery<MultiTypedBasicAttributesEntity> query = em.createQuery( criteria );
			query.setParameter( "p1", 1L );
			query.setParameter( "p2", 2L );
			query.setParameter( "p3", 3L );
			query.getResultList();
		} );
	}

	@Test
	@JiraKey("HHH-10870")
	public void testParameterInParameterList2(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = criteriaBuilder
					.createQuery( MultiTypedBasicAttributesEntity.class );

			final ParameterExpression<Iterable> parameter = criteriaBuilder.parameter( Iterable.class );

			final Root<MultiTypedBasicAttributesEntity> root = criteria.from( MultiTypedBasicAttributesEntity.class );
			criteria.select( root ).where( root.get( "id" ).in( parameter ) );

			final TypedQuery<MultiTypedBasicAttributesEntity> query1 = em.createQuery( criteria );
			query1.setParameter( parameter, Arrays.asList( 1L, 2L, 3L ) );
			query1.getResultList();
		} );
	}

	@Test
	@JiraKey("HHH-17912")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Blobs are not allowed in this expression")
	public void testAttributeEqualListParameter(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = criteriaBuilder
					.createQuery( MultiTypedBasicAttributesEntity.class );

			final ParameterExpression<List> parameter = criteriaBuilder.parameter( List.class );

			final Root<MultiTypedBasicAttributesEntity> root = criteria.from( MultiTypedBasicAttributesEntity.class );
			criteria.select( root ).where( criteriaBuilder.equal( root.get( MultiTypedBasicAttributesEntity_.integerList ), parameter ) );

			final TypedQuery<MultiTypedBasicAttributesEntity> query1 = em.createQuery( criteria );
			query1.setParameter( parameter, List.of( 1, 2, 3 ) );
			query1.getResultList();
		} );
	}
}
