/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.comments;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class UseSqlCommentTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class, TestEntity2.class };
	}

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.USE_SQL_COMMENTS, "true" );
		settings.put( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity testEntity = new TestEntity();
			testEntity.setId( "test1" );
			testEntity.setValue( "value1" );
			entityManager.persist( testEntity );

			TestEntity2 testEntity2 = new TestEntity2();
			testEntity2.setId( "test2" );
			testEntity2.setValue( "value2" );
			entityManager.persist( testEntity2 );
		} );
	}

	@Test
	public void testIt() {
		String appendLiteral = "*/select id as col_0_0_,value as col_1_0_ from testEntity2 where 1=1 or id=?--/*";
		doInJPA( this::entityManagerFactory, entityManager -> {

			List<TestEntity> result = findUsingQuery( "test1", appendLiteral, entityManager );

			TestEntity test1 = result.get( 0 );
			assertThat( test1.getValue(), is( appendLiteral ) );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {

			List<TestEntity> result = findUsingCriteria( "test1", appendLiteral, entityManager );

			TestEntity test1 = result.get( 0 );
			assertThat( test1.getValue(), is( appendLiteral ) );
		} );
	}

	public List<TestEntity> findUsingCriteria(String id, String appendLiteral, EntityManager entityManager) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<TestEntity> criteria = builder.createQuery( TestEntity.class );
		Root<TestEntity> root = criteria.from( TestEntity.class );

		Path<Object> idPath = root.get( "id" );
		CompoundSelection<TestEntity> selection = builder.construct(
				TestEntity.class,
				idPath,
				builder.literal( appendLiteral )
		);
		criteria.select( selection );

		criteria.where( builder.equal( idPath, builder.parameter( String.class, "where_id" ) ) );

		TypedQuery<TestEntity> query = entityManager.createQuery( criteria );
		query.setParameter( "where_id", id );
		return query.getResultList();
	}

	public List<TestEntity> findUsingQuery(String id, String appendLiteral, EntityManager entityManager) {
		TypedQuery<TestEntity> query =
				entityManager.createQuery(
						"select new org.hibernate.test.comments.TestEntity(id, '"
								+ appendLiteral.replace( "'", "''" )
								+ "') from TestEntity where id=:where_id",
						TestEntity.class
				);
		query.setParameter( "where_id", id );
		return query.getResultList();
	}
}
