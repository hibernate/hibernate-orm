/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Employee;
import org.hibernate.processor.test.integ.model.EmployeeInfo;
import org.hibernate.processor.test.integ.model.EmployeeSummary;
import org.hibernate.processor.test.integ.repository.EmployeeProjections;
import org.hibernate.processor.test.integ.repository._EmployeeProjections;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code @Query} methods without a SELECT clause that return
 * Java records as projections. The record component names determine
 * which entity attributes to retrieve.
 */
@DomainModel(
		annotatedClasses = { Employee.class, EmployeeProjections.class }
)
@SessionFactory
class RecordProjectionTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private void seedEmployees(_EmployeeProjections repo) {
		repo.save( new Employee( 1L, "Alice", "Engineering", 120_000.0 ) );
		repo.save( new Employee( 2L, "Bob", "Engineering", 110_000.0 ) );
		repo.save( new Employee( 3L, "Carol", "Marketing", 95_000.0 ) );
	}

	@Test
	void testRecordArray(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			EmployeeInfo[] infos = repo.infoArrayByDepartment( "Engineering" );
			assertEquals( 2, infos.length );
			assertEquals( "Alice", infos[0].name() );
			assertEquals( "Engineering", infos[0].department() );
			assertEquals( "Bob", infos[1].name() );
		} );
	}

	@Test
	void testRecordList(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			List<EmployeeInfo> infos = repo.infoListByDepartment( "Engineering" );
			assertEquals( 2, infos.size() );
			assertEquals( "Alice", infos.get( 0 ).name() );
			assertEquals( "Engineering", infos.get( 0 ).department() );
		} );
	}

	@Test
	void testRecordStream(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			try ( var stream = repo.infoStreamByDepartment( "Engineering" ) ) {
				List<EmployeeInfo> infos = stream.toList();
				assertEquals( 2, infos.size() );
				assertEquals( "Alice", infos.get( 0 ).name() );
				assertEquals( "Bob", infos.get( 1 ).name() );
			}
		} );
	}

	@Test
	void testSingleRecord(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			EmployeeInfo info = repo.infoById( 1L );
			assertNotNull( info );
			assertEquals( "Alice", info.name() );
			assertEquals( "Engineering", info.department() );
			assertEquals( 1L, info.id() );
		} );
	}

	@Test
	void testOptionalRecordPresent(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			Optional<EmployeeInfo> info = repo.optionalInfoById( 1L );
			assertTrue( info.isPresent() );
			assertEquals( "Alice", info.get().name() );
		} );
	}

	@Test
	void testOptionalRecordEmpty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );

			Optional<EmployeeInfo> info = repo.optionalInfoById( 999L );
			assertTrue( info.isEmpty() );
		} );
	}

	@Test
	void testRecordArrayEmpty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );

			EmployeeInfo[] infos = repo.infoArrayByDepartment( "Nonexistent" );
			assertEquals( 0, infos.length );
		} );
	}

	@Test
	void testSelectAnnotationList(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			List<EmployeeSummary> summaries = repo.summaryByDepartment( "Engineering" );
			assertEquals( 2, summaries.size() );
			assertEquals( "Alice", summaries.get( 0 ).employeeName() );
			assertEquals( "Engineering", summaries.get( 0 ).dept() );
			assertEquals( 120_000.0, summaries.get( 0 ).salary() );

			List<EmployeeSummary> positionalSummaries = repo.summaryByDepartmentPositional( "Engineering" );
			assertEquals( 2, positionalSummaries.size() );
			assertEquals( "Alice", positionalSummaries.get( 0 ).employeeName() );
			assertEquals( "Engineering", positionalSummaries.get( 0 ).dept() );
			assertEquals( 120_000.0, positionalSummaries.get( 0 ).salary() );
		} );
	}

	@Test
	void testSelectAnnotationOptionalPresent(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );
			seedEmployees( repo );

			Optional<EmployeeSummary> summary = repo.summaryById( 1L );
			assertTrue( summary.isPresent() );
			assertEquals( "Alice", summary.get().employeeName() );
			assertEquals( "Engineering", summary.get().dept() );
		} );
	}

	@Test
	void testSelectAnnotationOptionalEmpty(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _EmployeeProjections( session );

			Optional<EmployeeSummary> summary = repo.summaryById( 999L );
			assertTrue( summary.isEmpty() );
		} );
	}
}
