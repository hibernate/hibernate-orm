/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

/**
 * @author Gail Badner
 */

@JiraKey(value = "HHH-13492")
@ParameterizedClass
@MethodSource("parameters")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DomainModel(annotatedClasses = LockRefreshTest.Employee.class)
@SessionFactory
public class LockRefreshTest {
	public static Iterable<LockModeType> parameters() {
		return Arrays.asList(
				LockModeType.OPTIMISTIC,
				LockModeType.OPTIMISTIC_FORCE_INCREMENT
		);
	}

	private final LockModeType lockModeType;

	public LockRefreshTest(LockModeType lockModeType) {
		this.lockModeType = lockModeType;
	}

	@BeforeEach
	public void prepareTest(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employee = new Employee();
			employee.name = "Jane";
			employee.department = "Software Engineering";
			session.persist( employee );

		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testLockRefreshUpdate(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			session.lock( employee, lockModeType );
			session.refresh( employee );
			employee.department = "Finance";
		} );

		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			Assertions.assertEquals( "Finance", employee.department );
		} );
	}

	@Test
	public void testLockRefreshMerge(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			session.lock( employee, lockModeType );
			session.refresh( employee );
			employee.department = "Finance";
			session.merge( employee );
		} );

		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			Assertions.assertEquals( "Finance", employee.department );
		} );
	}

	@Test
	public void testLockRefreshDelete(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			session.lock( employee, lockModeType );
			session.refresh( employee );
			session.remove( employee );
		} );

		factoryScope.inTransaction( (session) -> {
			Assertions.assertNull( session.find( Employee.class, "Jane" ) );
		} );
	}

	@Test
	public void testLockRefreshEvict(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			session.lock( employee, lockModeType );
			session.refresh( employee );
			employee.department = "Finance";
			session.evict( employee );
		} );

		factoryScope.inTransaction( (session) -> {
			var employee = session.find( Employee.class, "Jane" );
			Assertions.assertEquals( "Software Engineering", employee.department );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		private String name;

		private String department;

		@Version
		@Column(name = "ver")
		private int version;
	}
}
