/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class ParameterTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		options.put( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	@Test
	public void testPrimitiveArrayParameterBinding() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
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
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNonPrimitiveArrayParameterBinding() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		CriteriaQuery<MultiTypedBasicAttributesEntity> criteria = em.getCriteriaBuilder()
				.createQuery( MultiTypedBasicAttributesEntity.class );
		Root<MultiTypedBasicAttributesEntity> rootEntity = criteria.from( MultiTypedBasicAttributesEntity.class );
		Path<Integer[]> thePath = rootEntity.get( MultiTypedBasicAttributesEntity_.someWrappedIntegers );
		ParameterExpression<Integer[]> param = em.getCriteriaBuilder().parameter( Integer[].class, "theIntegers" );
		criteria.where( em.getCriteriaBuilder().equal( thePath, param ) );
		TypedQuery<MultiTypedBasicAttributesEntity> query = em.createQuery( criteria );
		query.setParameter( param, new Integer[] {1, 1, 1} );
		assertThat( query.getParameterValue( param.getName() ), instanceOf( Integer[].class ) );
		query.getResultList();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNamedParameterMetadata() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
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

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testParameterInParameterList() {
		// Yes, this test makes no semantic sense.  But the JPA TCK does it...
		// 		it causes a problem on Derby, which does not like the form "... where ? in (?,?)"
		//		Derby wants one side or the other to be CAST (I assume so it can check typing).

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
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

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10870")
	public void testParameterInParameterList2() {
		TransactionUtil.doInJPA( this::entityManagerFactory, em -> {
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
	@TestForIssue(jiraKey = "HHH-17912")
	public void testAttributeEqualListParameter() {
		TransactionUtil.doInJPA( this::entityManagerFactory, em -> {
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

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { MultiTypedBasicAttributesEntity.class };
	}
}
