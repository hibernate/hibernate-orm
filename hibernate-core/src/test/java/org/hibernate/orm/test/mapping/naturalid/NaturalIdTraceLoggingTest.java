package org.hibernate.orm.test.mapping.naturalid;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Hibernate;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.CacheSettings;
import org.hibernate.engine.internal.NaturalIdLogging;

import org.hibernate.testing.logger.LogLevelContext;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.logger.LogLevelContext.withLevel;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		NaturalIdTraceLoggingTest.School.class,
		NaturalIdTraceLoggingTest.Teacher.class,
		NaturalIdTraceLoggingTest.Student.class,
		NaturalIdTraceLoggingTest.SchoolRecord.class,
		NaturalIdTraceLoggingTest.ArrayRecord.class
})
@ServiceRegistry(settings = @Setting(name = CacheSettings.USE_SECOND_LEVEL_CACHE, value = "true"))
@SessionFactory(useCollectingStatementObserver = true, generateStatistics = true)
@MessageKeyInspection(
		messageKey = "HHH09000",
		logger = @org.hibernate.testing.orm.junit.Logger(loggerName = NaturalIdLogging.LOGGER_NAME)
)
@Jira("HHH-20448")
class NaturalIdTraceLoggingTest {
	private Integer schoolId;
	private Integer studentId;
	private Integer recordId;

	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		try (var ignored = withLevel( NaturalIdLogging.LOGGER_NAME, INFO )) {
			scope.inTransaction( session -> {
				School school = new School( "High School" );
				session.persist( school );
				Teacher teacher = new Teacher( school, "John", "Doe" );
				session.persist( teacher );
				school.teachers = List.of( teacher );
				Student student = new Student( "Jane Doe", school );
				session.persist( student );
				SchoolRecord record = new SchoolRecord();
				record.school = school;
				session.persist( record );
				schoolId = school.identifier;
				studentId = student.identifier;
				recordId = record.identifier;
			} );
		}
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		try (var ignored = withLevel( NaturalIdLogging.LOGGER_NAME, INFO )) {
			scope.getSessionFactory().getCache().evictAllRegions();
			scope.getSessionFactory().getSchemaManager().truncate();
		}
	}

	@Test
	void loadingReporterGraphDoesNotTraverseCollection(SessionFactoryScope scope, MessageKeyWatcher watcher) {
		// A new session preserves the reporter's flush/clear boundary. School.teachers
		// is unidirectional, independently of the Teacher.school many-to-one.
		scope.inTransaction( session -> {
			Student student;
			try (var ignored = trace( scope, watcher )) {
				student = session.find( Student.class, studentId );
				assertFalse( Hibernate.isInitialized( student.school.teachers ) );
				assertTraceMessage( scope, watcher, "HHH090001" );
			}
			assertEquals( studentId, student.identifier );
			assertSame( student.school, session.find( School.class, schoolId ) );
			assertTrue( scope.getCollectingStatementObserver().getSqlQueries().stream()
					.noneMatch( sql -> sql.toLowerCase( java.util.Locale.ROOT ).contains( "trace_teacher" ) ) );
			assertCollectionContents( student.school );
		} );
	}

	static Stream<Arguments> tracePaths() {
		return Stream.concat(
				Stream.of( TraceOperation.values() ).flatMap( operation -> Stream.of(
						Arguments.of( operation, false, false ),
						Arguments.of( operation, true, true )
				) ),
				Stream.of( Arguments.of( TraceOperation.SESSION, true, false ) )
		);
	}

	@ParameterizedTest
	@MethodSource("tracePaths")
	void resolutionLoggingDoesNotInitializeAssociations(
			TraceOperation operation, boolean simple, boolean proxy,
			SessionFactoryScope scope, MessageKeyWatcher watcher) {
		scope.inTransaction( session -> {
			School school = proxy ? session.getReference( School.class, schoolId )
					: session.find( School.class, schoolId );
			assertEquals( !proxy, Hibernate.isInitialized( school ) );
			if ( !proxy ) {
				assertFalse( Hibernate.isInitialized( school.teachers ) );
			}
			var descriptor = session.getFactory().getMappingMetamodel()
					.getEntityDescriptor( simple ? SchoolRecord.class : Student.class );
			Object naturalId = simple ? school
					: descriptor.getNaturalIdMapping().extractNaturalIdFromEntity( new Student( "Jane Doe", school ) );
			Integer id = simple ? recordId : studentId;
			var resolutions = session.getPersistenceContext().getNaturalIdResolutions();
			if ( operation == TraceOperation.REMOVE || operation == TraceOperation.SESSION ) {
				resolutions.cacheResolution( id, naturalId, descriptor );
			}
			try (var ignored = trace( scope, watcher )) {
				switch ( operation ) {
					case LOAD -> resolutions.cacheResolutionFromLoad( id, naturalId, descriptor );
					case LOCAL -> assertTrue( resolutions.cacheResolution( id, naturalId, descriptor ) );
					case REMOVE -> {
						assertNotNull( resolutions.removeLocalResolution( id, naturalId, descriptor ) );
						assertNull( resolutions.findCachedNaturalIdById( id, descriptor ) );
					}
					case SESSION -> assertEquals( id, resolutions.findCachedIdByNaturalId( naturalId, descriptor ) );
					case DATABASE -> assertEquals( id, descriptor.getNaturalIdLoader().resolveNaturalIdToId( naturalId, session ) );
				}
				assertEquals( !proxy, Hibernate.isInitialized( school ), "Logging must not initialize the school proxy" );
				if ( !proxy ) {
					assertFalse( Hibernate.isInitialized( school.teachers ) );
				}
				assertEquals( operation == TraceOperation.DATABASE ? 1 : 0,
						scope.getCollectingStatementObserver().getSqlQueries().size() );
				assertTraceMessage( scope, watcher, operation.messageKey );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void secondLevelCacheLoggingDoesNotInitializeProxy(
			boolean simple, SessionFactoryScope scope, MessageKeyWatcher watcher) {
		Class<?> entityClass = simple ? SchoolRecord.class : Student.class;
		Integer id = simple ? recordId : studentId;
		scope.getSessionFactory().getCache().evictNaturalIdData( entityClass );
		scope.getSessionFactory().getStatistics().clear();
		try (var ignored = withLevel( NaturalIdLogging.LOGGER_NAME, INFO )) {
			// Warm the real natural-id cache through a load and committed transaction.
			scope.inTransaction( session -> {
				var access = session.byNaturalId( entityClass )
						.using( "school", session.getReference( School.class, schoolId ) );
				if ( !simple ) {
					access.using( "name", "Jane Doe" );
				}
				assertNotNull( access.load() );
			} );
		}
		assertTrue( scope.getSessionFactory().getStatistics().getNaturalIdCachePutCount() > 0 );
		scope.getSessionFactory().getStatistics().clear();
		scope.inTransaction( session -> {
			School school = session.getReference( School.class, schoolId );
			assertFalse( Hibernate.isInitialized( school ) );
			var descriptor = session.getFactory().getMappingMetamodel().getEntityDescriptor( entityClass );
			var resolutions = session.getPersistenceContext().getNaturalIdResolutions();
			assertNull( resolutions.findCachedNaturalIdById( id, descriptor ) );
			Object naturalId = simple ? school
					: descriptor.getNaturalIdMapping().extractNaturalIdFromEntity( new Student( "Jane Doe", school ) );
			try (var ignored = trace( scope, watcher )) {
				assertEquals( id, resolutions.findCachedIdByNaturalId( naturalId, descriptor ) );
				assertFalse( Hibernate.isInitialized( school ), "Logging must not initialize the school proxy" );
				assertTrue( scope.getCollectingStatementObserver().getSqlQueries().isEmpty() );
				assertEquals( 1, scope.getSessionFactory().getStatistics().getNaturalIdCacheHitCount() );
				assertTraceMessage( scope, watcher, "HHH090005" );
			}
		} );
	}

	static Stream<Arguments> scalarValues() {
		return Stream.of(
				Arguments.of( School.class, "basic-code", new String[] { "basic-code" } ),
				Arguments.of( School.class, null, new String[] { "null" } ),
				Arguments.of( ArrayRecord.class, new String[] { "alpha", "beta" }, new String[] { "alpha", "beta" } )
		);
	}

	@ParameterizedTest
	@MethodSource("scalarValues")
	void scalarValuesRemainReadable(
			Class<?> entityClass, Object value, String[] expected,
			SessionFactoryScope scope, MessageKeyWatcher watcher) {
		scope.inTransaction( session -> {
			var descriptor = session.getFactory().getMappingMetamodel().getEntityDescriptor( entityClass );
			if ( entityClass == ArrayRecord.class ) {
				assertEquals( 1, descriptor.getNaturalIdMapping().getNaturalIdAttributes().size() );
				assertTrue( descriptor.getNaturalIdMapping().isNormalized( value ) );
			}
			var resolutions = session.getPersistenceContext().getNaturalIdResolutions();
			try (var ignored = trace( scope, watcher )) {
				resolutions.cacheResolution( 1, value, descriptor );
				assertEquals( 1, resolutions.findCachedIdByNaturalId( value, descriptor ) );
				assertMessageContains( watcher, "HHH090002", expected );
				assertMessageContains( watcher, "HHH090004", expected );
			}
		} );
	}

	@Test
	void compoundNaturalIdMayContainNull(SessionFactoryScope scope, MessageKeyWatcher watcher) {
		scope.inTransaction( session -> {
			var descriptor = session.getFactory().getMappingMetamodel().getEntityDescriptor( Student.class );
			Object value = descriptor.getNaturalIdMapping().extractNaturalIdFromEntity( new Student( "nullable", null ) );
			try (var ignored = trace( scope, watcher )) {
				session.getPersistenceContext().getNaturalIdResolutions().cacheResolution( 1, value, descriptor );
				assertMessageContains( watcher, "HHH090002", "nullable", "null" );
			}
		} );
	}

	private static LogLevelContext trace(SessionFactoryScope scope, MessageKeyWatcher watcher) {
		watcher.reset();
		scope.getCollectingStatementObserver().clear();
		return withLevel( NaturalIdLogging.LOGGER_NAME, TRACE );
	}

	private static void assertMessageContains(MessageKeyWatcher watcher, String key, String... values) {
		assertTrue( watcher.getTriggeredMessages().stream().filter( message -> message.startsWith( key ) )
				.anyMatch( message -> Stream.of( values ).allMatch( message::contains ) ),
				"Expected the TRACE message to retain all mapped natural-ID values" );
	}

	private enum TraceOperation {
		LOAD("HHH090001"), LOCAL("HHH090002"), REMOVE("HHH090003"),
		SESSION("HHH090004"), DATABASE("HHH090006");

		private final String messageKey;

		TraceOperation(String messageKey) {
			this.messageKey = messageKey;
		}
	}

	private void assertTraceMessage(SessionFactoryScope scope, MessageKeyWatcher watcher, String key) {
		String schoolName = scope.getSessionFactory().getMappingMetamodel()
				.getEntityDescriptor( School.class ).getEntityName();
		assertTrue( watcher.getTriggeredMessages().stream()
				.anyMatch( message -> message.startsWith( key ) && message.contains( schoolName + "#" + schoolId ) ),
				"Expected the TRACE message to identify the school by entity name and identifier" );
	}

	private static void assertCollectionContents(School school) {
		assertFalse( Hibernate.isInitialized( school.teachers ) );
		Hibernate.initialize( school.teachers );
		assertEquals( 1, school.teachers.size() );
		Teacher teacher = school.teachers.iterator().next();
		assertEquals( "John", teacher.firstName );
		assertEquals( "Doe", teacher.lastName );
		assertSame( school, Hibernate.unproxy( teacher.school ) );
	}

	@Entity(name = "School")
	@Table(name = "trace_school")
	static class School {
		@Id
		@GeneratedValue
		private Integer identifier;
		@NaturalId
		private String name;
		@OneToMany
		private Collection<Teacher> teachers;

		School() {
		}

		School(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			throw new AssertionError( "Natural-ID TRACE invoked School.toString()" );
		}
	}

	@Entity(name = "Teacher")
	@Table(name = "trace_teacher")
	static class Teacher {
		@Id
		@GeneratedValue
		private Integer identifier;
		@ManyToOne
		@NaturalId
		private School school;
		@NaturalId
		private String firstName;
		@NaturalId
		private String lastName;

		Teacher() {
		}

		Teacher(School school, String firstName, String lastName) {
			this.school = school;
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	@NaturalIdCache
	@Entity(name = "Student")
	@Table(name = "trace_student")
	static class Student {
		@Id
		@GeneratedValue
		private Integer identifier;
		@NaturalId
		private String name;
		@ManyToOne
		@NaturalId
		private School school;

		Student() {
		}

		Student(String name, School school) {
			this.name = name;
			this.school = school;
		}
	}

	@NaturalIdCache
	@Entity(name = "SchoolRecord")
	@Table(name = "trace_record")
	static class SchoolRecord {
		@Id
		@GeneratedValue
		private Integer identifier;
		@ManyToOne
		@NaturalId
		private School school;
	}

	@Entity(name = "ArrayRecord")
	@Table(name = "trace_array")
	static class ArrayRecord {
		@Id
		private Integer identifier;
		@NaturalId
		private String[] labels;
	}

}
