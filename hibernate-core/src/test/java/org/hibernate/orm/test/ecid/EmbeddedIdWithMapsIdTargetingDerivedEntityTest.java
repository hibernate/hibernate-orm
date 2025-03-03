/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ecid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.hibernate.orm.test.util.SchemaUtil.getColumnNames;

/**
 * Test that bootstrap doesn't throw an exception
 * when an entity has an {@link EmbeddedId} and a {@link MapsId} that references a derived identity.
 * <p>
 * This test used to fail on bootstrap with the following error:
 * <p>
 * java.util.NoSuchElementException
 * at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1000)
 * at org.hibernate.cfg.annotations.TableBinder.linkJoinColumnWithValueOverridingNameIfImplicit(TableBinder.java:714)
 * at org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass.doSecondPass(PkDrivenByDefaultMapsIdSecondPass.java:37)
 * at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.processSecondPasses(InFlightMetadataCollectorImpl.java:1693)
 * at org.hibernate.boot.internal.InFlightMetadataCollectorImpl.processSecondPasses(InFlightMetadataCollectorImpl.java:1650)
 * at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.complete(MetadataBuildingProcess.java:295)
 * at org.hibernate.boot.model.process.spi.MetadataBuildingProcess.build(MetadataBuildingProcess.java:86)
 * at org.hibernate.boot.internal.MetadataBuilderImpl.build(MetadataBuilderImpl.java:479)
 * at org.hibernate.boot.internal.MetadataBuilderImpl.build(MetadataBuilderImpl.java:85)
 * at org.hibernate.cfg.Configuration.buildSessionFactory(Configuration.java:709)
 * at org.hibernate.testing.junit4.BaseCoreFunctionalTestCase.buildSessionFactory(BaseCoreFunctionalTestCase.java:125)
 * at org.hibernate.testing.junit4.BaseCoreFunctionalTestCase.buildSessionFactory(BaseCoreFunctionalTestCase.java:110)
 * at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
 * at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
 * at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
 * at java.base/java.lang.reflect.Method.invoke(Method.java:566)
 * at org.hibernate.testing.junit4.TestClassMetadata.performCallbackInvocation(TestClassMetadata.java:205)
 */
@JiraKey(value = "HHH-13295")
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class
		)
)
@DomainModel(
		annotatedClasses = {
				EmbeddedIdWithMapsIdTargetingDerivedEntityTest.Attendance.class,
				EmbeddedIdWithMapsIdTargetingDerivedEntityTest.AttendanceId.class,
				EmbeddedIdWithMapsIdTargetingDerivedEntityTest.Lecture.class,
				EmbeddedIdWithMapsIdTargetingDerivedEntityTest.Student.class,
				EmbeddedIdWithMapsIdTargetingDerivedEntityTest.User.class
		}
)
@SessionFactory
public class EmbeddedIdWithMapsIdTargetingDerivedEntityTest {

	@Test
	public void metadataTest(SessionFactoryScope scope) {
		assertThat( getColumnNames( "attendance", scope.getMetadataImplementor() ) )
				// Just check we're using @MapsId; otherwise the test wouldn't be able to reproduce HHH-13295.
				.containsExactlyInAnyOrder( "student_id", "lecture_id" );
	}

	// The main goal of the test is to check that bootstrap doesn't throw an exception,
	// but it feels wrong to have a test class with just an empty test method,
	// so just check that persisting/loading works correctly.
	@Test
	public void smokeTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Lecture lecture = new Lecture( 1L );
			s.persist( lecture );
			Student student = new Student( 2L );
			s.persist( student );
			Attendance attendance = new Attendance( lecture, student );
			student.getAttendances().add( attendance );
			lecture.getAttendances().add( attendance );
			s.persist( attendance );
		} );

		scope.inTransaction( s -> {
			Attendance attendance = s.get( Attendance.class, new AttendanceId( 1L, 2L ) );
			assertThat( attendance.getId() )
					.extracting( AttendanceId::getLectureId )
					.isEqualTo( 1L );
			assertThat( attendance.getId() )
					.extracting( AttendanceId::getStudentId )
					.isEqualTo( 2L );
			assertThat( attendance.getLecture() )
					.extracting( Lecture::getId )
					.isEqualTo( 1L );
			assertThat( attendance.getStudent() )
					.extracting( Student::getId )
					.isEqualTo( 2L );
		} );
	}

	@Entity
	@Table(name = "attendance")
	public static class Attendance {
		@EmbeddedId
		private AttendanceId id;

		@ManyToOne(fetch = FetchType.LAZY)
		@MapsId("lectureId")
		private Lecture lecture;

		@ManyToOne(fetch = FetchType.LAZY)
		@MapsId("studentId")
		private Student student;

		Attendance() {
		}

		public Attendance(Lecture lecture, Student student) {
			this.id = new AttendanceId( lecture.getId(), student.getId() );
			this.lecture = lecture;
			this.student = student;
		}

		public AttendanceId getId() {
			return id;
		}

		public Lecture getLecture() {
			return lecture;
		}

		public Student getStudent() {
			return student;
		}
	}

	@Embeddable
	public static class AttendanceId implements Serializable {
		@Column
		private Long lectureId;

		@Column
		private Long studentId;

		AttendanceId() {
		}

		public AttendanceId(Long lectureId, Long studentId) {
			this.lectureId = lectureId;
			this.studentId = studentId;
		}

		public Long getLectureId() {
			return lectureId;
		}

		public Long getStudentId() {
			return studentId;
		}
	}

	@Entity
	@Table(name = "users")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class User {
		@Id
		private Long id;

		User() {
		}

		public User(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity
	@Table(name = "student")
	public static class Student extends User {
		@OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
		private List<Attendance> attendances = new ArrayList<>();

		Student() {
		}

		public Student(Long id) {
			super( id );
		}

		public List<Attendance> getAttendances() {
			return attendances;
		}
	}

	@Entity
	@Table(name = "lecture")
	public static class Lecture {
		@Id
		private Long id;

		@OneToMany(mappedBy = "lecture", fetch = FetchType.LAZY)
		private List<Attendance> attendances = new ArrayList<>();

		Lecture() {
		}

		public Lecture(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Attendance> getAttendances() {
			return attendances;
		}
	}
}
