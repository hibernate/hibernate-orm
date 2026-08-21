/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Jpa(
		annotatedClasses = {
				IdClassAndAssociationsTest.CourseEnrollment.class,
				IdClassAndAssociationsTest.Unit.class,
				IdClassAndAssociationsTest.User.class,
				IdClassAndAssociationsTest.UserRole.class
		}
)
@JiraKey( value = "HHH-16075")
public class IdClassAndAssociationsTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Unit unit = new Unit( "unit 1" );
					User trainee = new User( "Roy", unit );

					UserRole userRole = new UserRole( trainee, "Trainee" );

					trainee.addUserRole( userRole );

					CourseEnrollment courseEnrollment = new CourseEnrollment( 1l, trainee );

					entityManager.persist( unit );
					entityManager.persist( trainee );
					entityManager.persist( userRole );

					entityManager.persist( courseEnrollment );
				}
		);
	}

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final List<CourseEnrollment> enrollments = entityManager.createQuery(
									"SELECT c FROM CourseEnrollment c WHERE c.courseId = ?1 AND c.trainee.firstName like (?2)",
									CourseEnrollment.class
							)
							.setParameter( 1, 1L )
							.setParameter( 2, "Roy" )
							.getResultList();

					assertEquals( 1, enrollments.size() );

					User trainer = enrollments.get( 0 ).getTrainee();
					assertNotNull( trainer );
					Set<UserRole> roles = trainer.getRoles();
				}
		);

	}

	@Entity(name = "CourseEnrollment")
	@Table(name = "course_enrollment")
	public static class CourseEnrollment {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@Column(name = "course")
		Long courseId;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "trainee", referencedColumnName = "id")
		User trainee;

		public CourseEnrollment() {
		}

		public CourseEnrollment(Long courseId, User trainee) {
			this.courseId = courseId;
			this.trainee = trainee;
		}

		public Long getId() {
			return id;
		}

		public Long getCourseId() {
			return courseId;
		}

		public User getTrainee() {
			return trainee;
		}

	}

	@Entity(name = "Unit")
	@Table(name = "units")
	public static class Unit {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;

		public Unit() {
		}

		public Unit(String name) {
			this.name = name;
		}
	}

	@Entity(name = "User")
	@Table(name = "users")
	public static class User {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		@Column(name = "first_name")
		private String firstName;

		@ManyToOne
		@JoinColumn(name = "unit")
		private Unit unit;

		@JoinColumn(name = "user_id", referencedColumnName = "id")
		@OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
		private Set<UserRole> roles = new HashSet<>();

		public User() {
		}

		public User(String firstName, Unit unit) {
			this.firstName = firstName;
			this.unit = unit;
		}

		public Long getId() {
			return id;
		}

		public Set<UserRole> getRoles() {
			return roles;
		}

		public void addUserRole(UserRole role) {
			this.roles.add( role );
		}
	}

	@Entity(name = "UserRole")
	@Table(name = "users_roles")
	@IdClass(UserRoleId.class)
	public static class UserRole {

		@Id
		@ManyToOne
		@JoinColumn(name = "user_id")
		private User user;

		@Id
		@Column(name = "role_name")
		private String roleName;

		public UserRole() {
		}

		public UserRole(User user, String roleName) {
			this.user = user;
			this.roleName = roleName;
		}
	}

	public static class UserRoleId implements Serializable {

		private User user;
		private String roleName;

		public UserRoleId() {
		}

		public UserRoleId(User user, String roleName) {
			this.user = user;
			this.roleName = roleName;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

		public String getRoleName() {
			return roleName;
		}

		public void setRoleName(String roleName) {
			this.roleName = roleName;
		}
	}
}
