/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.cfg.QuerySettings;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;
import org.hibernate.testing.orm.junit.SessionFactory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel (annotatedClasses = {SafeModeDebugTest.TestEntity.class})
@ServiceRegistry (settings = @Setting( name = QuerySettings.SAFE_MODE_ENABLED, value = "true"))
public class SafeModeDebugTest {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HQL Tests
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testHqlFunctionWithUnknownFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( IllegalArgumentException.class,
					() -> session.createQuery( "select function('REGEXP_SUBSTR', e.name) from TestEntity e", String.class )
							.getResultList() );

			assertTrue( ex.getMessage().contains( "Unknown function [regexp_substr] is not allowed in safe mode" ) );
		} );
	}

	@Test
	public void testHqlUnknownFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( IllegalArgumentException.class,
					() -> session.createQuery( "select REGEXP_SUBSTR(e.name) from TestEntity e", String.class )
							.getResultList() );

			assertTrue( ex.getMessage().contains( "Unknown function [regexp_substr] is not allowed in safe mode" ) );
		} );
	}

	@Test
	public void testHqlColumnFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( IllegalArgumentException.class,
					() -> session.createQuery( "select column(e.name) from TestEntity e", String.class )
							.getResultList() );

			assertTrue( ex.getMessage().contains( "Function [column] is not allowed in safe mode" ) );
		} );
	}

	@Test
	public void testHqlSqlFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Exception ex = assertThrows( IllegalArgumentException.class,
					() -> session.createQuery( "select sql('SELECT 1') from TestEntity e", String.class )
							.getResultList() );

			assertTrue( ex.getMessage().contains( "Function [sql] is not allowed in safe mode" ) );
		} );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// CriteriaBuilder Tests
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Test
	public void testCriteriaUnknownFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			JpaCriteriaQuery<String> query = criteriaBuilder.createQuery( String.class );
			JpaRoot<TestEntity> from = query.from( TestEntity.class );
			JpaPath<String> name = from.get( SafeModeDebugTest_.TestEntity_.name );

					Exception ex = assertThrows( SemanticException.class,
							() -> {
								query.select( criteriaBuilder.function( "REGEXP_SUBSTR", String.class, name ) );
								session.createQuery( query ).getResultList();
							} );

							assertTrue( ex.getMessage().contains( "Unknown function [REGEXP_SUBSTR] is not allowed in safe mode" ) );
		} );
	}

	@Test
	public void testCriteriaBuilderSqlFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			JpaCriteriaQuery<String> query = criteriaBuilder.createQuery( String.class );
			JpaRoot<TestEntity> from = query.from( TestEntity.class );
			JpaPath<String> name = from.get( SafeModeDebugTest_.TestEntity_.name );

			Exception ex = assertThrows( SemanticException.class,
					() -> {
						query.select( criteriaBuilder.function( "sql", String.class, name ) );
						session.createQuery( query ).getResultList();
					} );

			assertTrue( ex.getMessage().contains( "Function [sql] is not allowed in safe mode" ) );
		} );
	}

	@Test
	public void testCriteriaBuilderColumnFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var criteriaBuilder = session.getCriteriaBuilder();
			JpaCriteriaQuery<String> query = criteriaBuilder.createQuery( String.class );
			JpaRoot<TestEntity> from = query.from( TestEntity.class );
			JpaPath<String> name = from.get( SafeModeDebugTest_.TestEntity_.name );

			Exception ex = assertThrows( SemanticException.class,
					() -> {
						query.select( criteriaBuilder.function( "column", String.class, name ) );
						session.createQuery( query ).getResultList();
					} );

			assertTrue( ex.getMessage().contains( "Unknown function [column] is not allowed in safe mode" ) );
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		Long id;
		String name;
	}
}
