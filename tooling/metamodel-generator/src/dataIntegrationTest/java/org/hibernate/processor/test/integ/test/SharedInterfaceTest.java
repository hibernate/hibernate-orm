/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Employee;
import org.hibernate.processor.test.integ.model.Project;
import org.hibernate.processor.test.integ.repository.EmployeeDirectory;
import org.hibernate.processor.test.integ.repository.ProjectRegistry;
import org.hibernate.processor.test.integ.repository._EmployeeDirectory;
import org.hibernate.processor.test.integ.repository._ProjectRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.restrict.Restrict;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the pattern where a shared (non-{@code @Repository}) interface
 * defines {@code @Query} methods that are inherited by two concrete
 * {@code @Repository} interfaces backed by different entities.
 */
@DomainModel(
		annotatedClasses = {
				Employee.class, Project.class,
				EmployeeDirectory.class, ProjectRegistry.class
		}
)
@SessionFactory
class SharedInterfaceTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testCountByDepartment(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 110_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Marketing", 95_000.0 ) );

			assertEquals( 2, dir.countEmployeesByDepartment( "Engineering" ) );
			assertEquals( 1, dir.countEmployeesByDepartment( "Marketing" ) );
			assertEquals( 0, dir.countEmployeesByDepartment( "Sales" ) );
		} );
	}

	@Test
	void testEmployeeSpecificQuery(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 80_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Marketing", 150_000.0 ) );

			List<Employee> earners = dir.highEarners( 100_000.0 );
			assertEquals( 2, earners.size() );
			assertEquals( "Carol", earners.get( 0 ).getName() );
			assertEquals( "Alice", earners.get( 1 ).getName() );
		} );
	}

	@Test
	void testProjectSpecificQuery(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var reg = new _ProjectRegistry( session );
			reg.save( new Project( 1L, "Atlas", "Engineering", 500_000.0 ) );
			reg.save( new Project( 2L, "Brand Refresh", "Marketing", 50_000.0 ) );
			reg.save( new Project( 3L, "Beacon", "Engineering", 300_000.0 ) );

			List<Project> funded = reg.fundedAbove( 100_000.0 );
			assertEquals( 2, funded.size() );
			assertEquals( "Atlas", funded.get( 0 ).getName() );
			assertEquals( "Beacon", funded.get( 1 ).getName() );
		} );
	}

	@Test
	void testSortVarargs(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 80_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Marketing", 150_000.0 ) );

			try ( var stream = dir.sortedByDepartment( "Engineering", Sort.desc( "salary" ) ) ) {
				List<Employee> result = stream.toList();
				assertEquals( 2, result.size() );
				assertEquals( "Alice", result.get( 0 ).getName() );
				assertEquals( "Bob", result.get( 1 ).getName() );
			}
		} );
	}

	@Test
	void testOrder(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 80_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Engineering", 95_000.0 ) );

			List<Employee> result = dir.orderedByDepartment( "Engineering",
					Order.by( Sort.asc( "name" ) ) );
			assertEquals( 3, result.size() );
			assertEquals( "Alice", result.get( 0 ).getName() );
			assertEquals( "Bob", result.get( 1 ).getName() );
			assertEquals( "Carol", result.get( 2 ).getName() );
		} );
	}

	@Test
	void testLimitWithSortVarargs(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 80_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Engineering", 95_000.0 ) );

			List<Employee> result = dir.pagedByDepartment( "Engineering",
					Limit.of( 2 ), Sort.desc( "salary" ) );
			assertEquals( 2, result.size() );
			assertEquals( "Alice", result.get( 0 ).getName() );
			assertEquals( "Carol", result.get( 1 ).getName() );
		} );
	}

	@Test
	void testRestriction(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var dir = new _EmployeeDirectory( session );
			dir.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
			dir.save( new Employee( 2L, "Bob", "Engineering", 80_000.0 ) );
			dir.save( new Employee( 3L, "Carol", "Marketing", 150_000.0 ) );

			List<Employee> result = dir.allEmployees( Restrict.unrestricted() );
			assertEquals( 3, result.size() );
		} );
	}
}
