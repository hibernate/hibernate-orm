/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				RemoveEntityTest.EmployeeEntity.class,
				RemoveEntityTest.LinkEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16816")
public class RemoveEntityTest {

	private static final String EMPLOYEE_TO_DELETE_MAIL = "demo-user@mail.com";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EmployeeEntity emp = new EmployeeEntity();
					emp.setEmail( EMPLOYEE_TO_DELETE_MAIL );
					session.persist( emp );

					LinkEntity linkEntity = new LinkEntity();
					linkEntity.setEmployeeId( emp.getEmployeeId() );
					session.persist( linkEntity );

					Set<LinkEntity> link = Set.of( linkEntity );
					emp.setFolderLink( link );
					session.persist( emp );

					emp = new EmployeeEntity();
					emp.setEmail( "demo-user2@mail.com" );
					session.persist( emp );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EmployeeEntity employee = session.createQuery(
							"FROM EmployeeEntity e where e.email = :mail",
							EmployeeEntity.class
					).setParameter( "mail", EMPLOYEE_TO_DELETE_MAIL ).getSingleResult();

					assertThat( employee ).isNotNull();

					List<LinkEntity> links = session.createQuery( "FROM LinkEntity", LinkEntity.class ).list();
					assertThat( links.size() ).isEqualTo( 1 );

					LinkEntity linkEntity = links.get( 0 );
					assertThat( linkEntity.getEmployeeId() ).isEqualTo( employee.getEmployeeId() );

					session.remove( linkEntity );
					session.remove( employee );
				}
		);

		scope.inTransaction(
				session -> {
					List<EmployeeEntity> employees = session.createQuery(
							"FROM EmployeeEntity e where e.email = :mail",
							EmployeeEntity.class
					).setParameter( "mail", EMPLOYEE_TO_DELETE_MAIL ).getResultList();

					assertThat( employees.size() ).isEqualTo( 0 );

					List<LinkEntity> links = session.createQuery( "FROM LinkEntity", LinkEntity.class ).list();
					assertThat( links.size() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "EmployeeEntity")
	@Table(name = "Employee", uniqueConstraints = {
			@UniqueConstraint(columnNames = "ID"),
			@UniqueConstraint(columnNames = "EMAIL")
	})
	public static class EmployeeEntity {

		@Id
		@GeneratedValue
		@Column(name = "ID")
		private Integer employeeId;

		@Column(name = "EMAIL", unique = true, nullable = false, length = 100)
		private String email;

		@OneToMany(targetEntity = LinkEntity.class, mappedBy = "employee")
		final Set<LinkEntity> folderLink = new HashSet<>();

		public Collection<LinkEntity> getFolderLink() {
			return folderLink;
		}

		public void setFolderLink(Collection<LinkEntity> folderLink) {
			final HashSet<LinkEntity> changed = new HashSet<>( folderLink );
			changed.removeAll( this.folderLink );
			changed.forEach( added -> added.setEmployee( this ) );
			changed.clear();
			changed.addAll( this.folderLink );
			changed.removeAll( folderLink );
			changed.forEach( removed -> removed.setEmployee( null ) );
		}

		public Integer getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(Integer employeeId) {
			this.employeeId = employeeId;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}
	}

	@Entity(name = "LinkEntity")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@Table(name = "Link")
	public static class LinkEntity {

		@Id
		@Column(name = "EMPLOYEE_ID")
		private Integer employeeId;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "EMPLOYEE_ID", insertable = false, updatable = false)
		EmployeeEntity employee;

		public EmployeeEntity getEmployee() {
			return employee;
		}

		public void setEmployee(EmployeeEntity realFolder) {
			this.employee = realFolder;
		}

		public Integer getEmployeeId() {
			return employeeId;
		}

		public void setEmployeeId(Integer employeeId) {
			this.employeeId = employeeId;
		}
	}

}
