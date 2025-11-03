/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests HQL and Criteria queries using DB columns having the same name as registered functions.
 *
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "HHH-6426")
@SkipForDialect(dialectClass = PostgresPlusDialect.class,
		reason = "Almost all of the tests result in 'ambiguous column' errors.")
@ServiceRegistry(settings = @Setting(name = USE_QUERY_CACHE, value = "false"))
@DomainModel(xmlMappings = "org/hibernate/orm/test/hql/FunctionNamesAsColumns.hbm.xml")
@SessionFactory
public class FunctionNameAsColumnTest {

	@AfterEach
	public void cleanup(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionHQL(SessionFactoryScope sessionFactory) {
		sessionFactory.inTransaction( s -> {
			EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
			e.setLower( 3 );
			e.setUpper( " abc " );
			s.persist( e );
		} );

		sessionFactory.inTransaction(s -> {
			EntityWithArgFunctionAsColumn e = (EntityWithArgFunctionAsColumn) s.createQuery(
					"from EntityWithArgFunctionAsColumn" ).uniqueResult();
			assertEquals( 3, e.getLower() );
			assertEquals( " abc ", e.getUpper() );
		} );
	}

	@Test
	public void testGetOneColumnSameNameAsArgFunctionCriteria(SessionFactoryScope sessionFactory) {
		sessionFactory.inTransaction(s -> {
			EntityWithArgFunctionAsColumn e = new EntityWithArgFunctionAsColumn();
			e.setLower( 3 );
			e.setUpper( " abc " );
			s.persist( e );
		} );

		sessionFactory.inTransaction(s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<EntityWithArgFunctionAsColumn> criteria = criteriaBuilder.createQuery(
					EntityWithArgFunctionAsColumn.class );
			criteria.from( EntityWithArgFunctionAsColumn.class );

			EntityWithArgFunctionAsColumn e = s.createQuery( criteria ).uniqueResult();

			assertEquals( 3, e.getLower() );
			assertEquals( " abc ", e.getUpper() );
		} );
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionHQL(SessionFactoryScope sessionFactory) {
		sessionFactory.inTransaction(s -> {
			EntityWithArgFunctionAsColumn e1 = new EntityWithArgFunctionAsColumn();
			e1.setLower( 3 );
			e1.setUpper( " abc " );
			EntityWithArgFunctionAsColumn e2 = new EntityWithArgFunctionAsColumn();
			e2.setLower( 999 );
			e2.setUpper( " xyz " );
			EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
			holder1.getEntityWithArgFunctionAsColumns().add( e1 );
			EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
			holder2.getEntityWithArgFunctionAsColumns().add( e2 );
			holder1.setNextHolder( holder2 );
			s.persist( holder1 );
		} );

		sessionFactory.inTransaction(s -> {
			EntityWithFunctionAsColumnHolder holder1 = (EntityWithFunctionAsColumnHolder) s.createQuery(
					"from EntityWithFunctionAsColumnHolder h left join fetch h.entityWithArgFunctionAsColumns " +
							"join fetch h.nextHolder left join fetch h.nextHolder.entityWithArgFunctionAsColumns " +
							"where h.nextHolder is not null" )
					.uniqueResult();
			assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
			assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
			EntityWithArgFunctionAsColumn e1 = (EntityWithArgFunctionAsColumn) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
			assertEquals( 3, e1.getLower() );
			assertEquals( " abc ", e1.getUpper() );
			assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
			EntityWithArgFunctionAsColumn e2 = (EntityWithArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( 999, e2.getLower() );
			assertEquals( " xyz ", e2.getUpper() );
		} );
	}

	@Test
	public void testGetMultiColumnSameNameAsArgFunctionCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			EntityWithArgFunctionAsColumn e1 = new EntityWithArgFunctionAsColumn();
			e1.setLower( 3 );
			e1.setUpper( " abc " );
			EntityWithArgFunctionAsColumn e2 = new EntityWithArgFunctionAsColumn();
			e2.setLower( 999 );
			e2.setUpper( " xyz " );
			EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
			holder1.getEntityWithArgFunctionAsColumns().add( e1 );
			EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
			holder2.getEntityWithArgFunctionAsColumns().add( e2 );
			holder1.setNextHolder( holder2 );
			s.persist( holder1 );
		} );

		factoryScope.inTransaction(	s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<EntityWithFunctionAsColumnHolder> criteria = criteriaBuilder.createQuery(
					EntityWithFunctionAsColumnHolder.class );
			Root<EntityWithFunctionAsColumnHolder> root = criteria.from( EntityWithFunctionAsColumnHolder.class );
			root.fetch( "entityWithArgFunctionAsColumns", JoinType.LEFT );
			root.fetch( "nextHolder", JoinType.LEFT )
					.fetch( "entityWithArgFunctionAsColumns", JoinType.LEFT );
			criteria.where( criteriaBuilder.isNotNull( root.get( "nextHolder" ) ) );

			EntityWithFunctionAsColumnHolder holder1 = s.createQuery( criteria ).uniqueResult();

			assertTrue( Hibernate.isInitialized( holder1.getEntityWithArgFunctionAsColumns() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithArgFunctionAsColumns() ) );
			assertEquals( 1, holder1.getEntityWithArgFunctionAsColumns().size() );
			EntityWithArgFunctionAsColumn e1 = (EntityWithArgFunctionAsColumn) holder1.getEntityWithArgFunctionAsColumns().iterator().next();
			assertEquals( 3, e1.getLower() );
			assertEquals( " abc ", e1.getUpper() );
			assertEquals( 1, holder1.getNextHolder().getEntityWithArgFunctionAsColumns().size() );
			EntityWithArgFunctionAsColumn e2 = (EntityWithArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( 999, e2.getLower() );
			assertEquals( " xyz ", e2.getUpper() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Ambiguous column")
	public void testGetMultiColumnSameNameAsNoArgFunctionHQL(SessionFactoryScope factoryScope) {
		Assumptions.assumeFalse( dialectUsesParenForCurrentDate( factoryScope ),
				"current_date requires () but test is for noarg function that does not require ()" );

		factoryScope.inTransaction(s -> {
			EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
			e1.setCurrentDate( "blah blah blah" );
			EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
			e2.setCurrentDate( "yadda yadda yadda" );
			EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
			holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
			EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
			holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
			holder1.setNextHolder( holder2 );
			s.persist( holder1 );
		} );

		factoryScope.inTransaction(	s -> {
			var hql = """
					from EntityWithFunctionAsColumnHolder h
						left join fetch h.entityWithNoArgFunctionAsColumns
						join fetch h.nextHolder
						left join fetch h.nextHolder.entityWithNoArgFunctionAsColumns
					where h.nextHolder is not null
					""";
			var holder1 = s.createQuery( hql, EntityWithFunctionAsColumnHolder.class ).uniqueResult();
			assertTrue( Hibernate.isInitialized( holder1.getEntityWithNoArgFunctionAsColumns() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder() ) );
			assertTrue( Hibernate.isInitialized( holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns() ) );
			assertEquals( 1, holder1.getEntityWithNoArgFunctionAsColumns().size() );

			EntityWithNoArgFunctionAsColumn e1 = (EntityWithNoArgFunctionAsColumn) holder1.getEntityWithNoArgFunctionAsColumns().iterator().next();
			assertEquals( "blah blah blah", e1.getCurrentDate() );
			assertEquals( 1, holder1.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
			EntityWithNoArgFunctionAsColumn e2 = (EntityWithNoArgFunctionAsColumn) ( holder1.getNextHolder() ).getEntityWithNoArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( "yadda yadda yadda", e2.getCurrentDate() );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Ambiguous column")
	public void testGetMultiColumnSameNameAsNoArgFunctionCriteria(SessionFactoryScope factoryScope) {
		Assumptions.assumeFalse( dialectUsesParenForCurrentDate( factoryScope ),
				"current_date requires () but test is for noarg function that does not require ()" );

		factoryScope.inTransaction(	s -> {
			EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
			EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();

			EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
			e1.setCurrentDate( "blah blah blah" );
			EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();
			e2.setCurrentDate( "yadda yadda yadda" );
			holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
			holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
			holder1.setNextHolder( holder2 );
			s.persist( holder1 );
		} );

		factoryScope.inTransaction(s -> {
			CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
			CriteriaQuery<EntityWithFunctionAsColumnHolder> criteria = criteriaBuilder.createQuery(
					EntityWithFunctionAsColumnHolder.class );
			Root<EntityWithFunctionAsColumnHolder> root = criteria.from( EntityWithFunctionAsColumnHolder.class );
			root.fetch( "entityWithNoArgFunctionAsColumns", JoinType.LEFT );
			root.fetch( "nextHolder", JoinType.LEFT )
					.fetch( "entityWithNoArgFunctionAsColumns", JoinType.LEFT );
			criteria.where( criteriaBuilder.isNotNull( root.get( "nextHolder" ) ) );
			EntityWithFunctionAsColumnHolder holder = s.createQuery( criteria ).uniqueResult();

			assertTrue( Hibernate.isInitialized( holder.getEntityWithNoArgFunctionAsColumns() ) );
			assertTrue( Hibernate.isInitialized( holder.getNextHolder() ) );
			assertTrue( Hibernate.isInitialized( holder.getNextHolder()
														.getEntityWithNoArgFunctionAsColumns() ) );
			assertEquals( 1, holder.getEntityWithNoArgFunctionAsColumns().size() );
			EntityWithNoArgFunctionAsColumn e1 = (EntityWithNoArgFunctionAsColumn) holder.getEntityWithNoArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( "blah blah blah", e1.getCurrentDate() );
			assertEquals( 1, holder.getNextHolder().getEntityWithNoArgFunctionAsColumns().size() );
			EntityWithNoArgFunctionAsColumn e2 = (EntityWithNoArgFunctionAsColumn) ( holder.getNextHolder() ).getEntityWithNoArgFunctionAsColumns()
					.iterator()
					.next();
			assertEquals( "yadda yadda yadda", e2.getCurrentDate() );
		} );
	}

	@Test
	public void testNoArgFcnAndColumnSameNameAsNoArgFunctionHQL(SessionFactoryScope factoryScope) {
		Assumptions.assumeFalse( dialectUsesParenForCurrentDate( factoryScope ),
				"current_date requires () but test is for noarg function that does not require ()" );

		MutableObject<EntityWithNoArgFunctionAsColumn> e1Ref = new MutableObject<>();
		MutableObject<EntityWithNoArgFunctionAsColumn> e2Ref = new MutableObject<>();

		factoryScope.inTransaction( (s) -> {
			EntityWithNoArgFunctionAsColumn e1 = new EntityWithNoArgFunctionAsColumn();
			EntityWithNoArgFunctionAsColumn e2 = new EntityWithNoArgFunctionAsColumn();

			e1Ref.set( e1 );
			e2Ref.set( e2 );

			e1.setCurrentDate( "blah blah blah" );
			e2.setCurrentDate( "yadda yadda yadda" );
			EntityWithFunctionAsColumnHolder holder1 = new EntityWithFunctionAsColumnHolder();
			holder1.getEntityWithNoArgFunctionAsColumns().add( e1 );
			EntityWithFunctionAsColumnHolder holder2 = new EntityWithFunctionAsColumnHolder();
			holder2.getEntityWithNoArgFunctionAsColumns().add( e2 );
			holder1.setNextHolder( holder2 );
			s.persist( holder1 );
		} );

		factoryScope.inTransaction(s -> {
			var hql = "select str(current_date), currentDate from EntityWithNoArgFunctionAsColumn";
			var results = s.createQuery( hql ).list();
			assertEquals( 2, results.size() );
			assertEquals( ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0] );
			assertNotEquals( ((Object[]) results.get( 0 ))[0], ((Object[]) results.get( 0 ))[1] );
			assertNotEquals( ((Object[]) results.get( 1 ))[0], ((Object[]) results.get( 1 ))[1] );
			assertTrue( ( (Object[]) results.get( 0 ) )[1].equals( e1Ref.get().getCurrentDate() ) ||
								( (Object[]) results.get( 0 ) )[1].equals( e2Ref.get().getCurrentDate() ) );
			assertTrue( ( (Object[]) results.get( 1 ) )[1].equals( e1Ref.get().getCurrentDate() ) ||
								( (Object[]) results.get( 1 ) )[1].equals( e2Ref.get().getCurrentDate() ) );
			assertNotEquals( ((Object[]) results.get( 0 ))[1], ((Object[]) results.get( 1 ))[1] );
		} );
	}

	private static boolean dialectUsesParenForCurrentDate(SessionFactoryScope factoryScope) {
		return factoryScope.getSessionFactory().getJdbcServices().getDialect().currentDate().contains( "(" );
	}
}
