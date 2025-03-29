/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.comments;

import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

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
						"select new " + TestEntity.class.getName() + "(id, '"
								+ appendLiteral.replace( "'", "''" )
								+ "') from TestEntity where id=:where_id",
						TestEntity.class
				);
		query.setParameter( "where_id", id );
		return query.getResultList();
	}
}
