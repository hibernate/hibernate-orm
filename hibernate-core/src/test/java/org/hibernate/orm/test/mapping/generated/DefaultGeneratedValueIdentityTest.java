/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.OnExecutionGenerator;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SkipForDialect(value = SybaseDialect.class, comment = "CURRENT_TIMESTAMP not supported as default value in Sybase")
@SkipForDialect(value = MySQLDialect.class, comment = "See HHH-10196")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class DefaultGeneratedValueIdentityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Override
	protected boolean isCleanupTestDataUsingBulkDelete() {
		return true;
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12671" )
	public void testGenerationWithIdentityInsert() {
		final TheEntity theEntity = new TheEntity();

		doInHibernate( this::sessionFactory, session -> {
			assertNull( theEntity.createdDate );
			assertNull( theEntity.alwaysDate );
			assertNull( theEntity.vmCreatedDate );
			assertNull( theEntity.vmCreatedSqlDate );
			assertNull( theEntity.vmCreatedSqlTime );
			assertNull( theEntity.vmCreatedSqlTimestamp );

			assertNull( theEntity.vmCreatedSqlLocalDate );
			assertNull( theEntity.vmCreatedSqlLocalTime );
			assertNull( theEntity.vmCreatedSqlLocalDateTime );
			assertNull( theEntity.vmCreatedSqlMonthDay );
			assertNull( theEntity.vmCreatedSqlOffsetDateTime );
			assertNull( theEntity.vmCreatedSqlOffsetTime );
			assertNull( theEntity.vmCreatedSqlYear );
			assertNull( theEntity.vmCreatedSqlYearMonth );
			assertNull( theEntity.vmCreatedSqlZonedDateTime );
			assertNull( theEntity.dbCreatedDate );

			assertNull( theEntity.name );
			session.persist( theEntity );

			assertNotNull( theEntity.createdDate );
			assertNotNull( theEntity.alwaysDate );
			assertNotNull( theEntity.vmCreatedDate );
			assertNotNull( theEntity.vmCreatedSqlDate );
			assertNotNull( theEntity.vmCreatedSqlTime );
			assertNotNull( theEntity.vmCreatedSqlTimestamp );
			assertNotNull( theEntity.vmCreatedSqlLocalDate );
			assertNotNull( theEntity.vmCreatedSqlLocalTime );
			assertNotNull( theEntity.vmCreatedSqlLocalDateTime );
			assertNotNull( theEntity.vmCreatedSqlMonthDay );
			assertNotNull( theEntity.vmCreatedSqlOffsetDateTime );
			assertNotNull( theEntity.vmCreatedSqlOffsetTime );
			assertNotNull( theEntity.vmCreatedSqlYear );
			assertNotNull( theEntity.vmCreatedSqlYearMonth );
			assertNotNull( theEntity.vmCreatedSqlZonedDateTime );
			assertNotNull( theEntity.dbCreatedDate );
			assertNotNull( theEntity.name );
		} );

		assertNotNull( theEntity.createdDate );
		assertNotNull( theEntity.alwaysDate );
		assertEquals( "Bob", theEntity.name );

		doInHibernate( this::sessionFactory, session -> {
			TheEntity _theEntity = session.get( TheEntity.class, theEntity.id );
			assertNotNull( _theEntity.createdDate );
			assertNotNull( _theEntity.alwaysDate );
			assertNotNull( _theEntity.vmCreatedDate );
			assertNotNull( _theEntity.vmCreatedSqlDate );
			assertNotNull( _theEntity.vmCreatedSqlTime );
			assertNotNull( _theEntity.vmCreatedSqlTimestamp );
			assertNotNull( _theEntity.vmCreatedSqlLocalDate );
			assertNotNull( _theEntity.vmCreatedSqlLocalTime );
			assertNotNull( _theEntity.vmCreatedSqlLocalDateTime );
			assertNotNull( _theEntity.vmCreatedSqlMonthDay );
			assertNotNull( _theEntity.vmCreatedSqlOffsetDateTime );
			assertNotNull( _theEntity.vmCreatedSqlOffsetTime );
			assertNotNull( _theEntity.vmCreatedSqlYear );
			assertNotNull( _theEntity.vmCreatedSqlYearMonth );
			assertNotNull( _theEntity.vmCreatedSqlZonedDateTime );
			assertNotNull( _theEntity.dbCreatedDate );
			assertEquals( "Bob", _theEntity.name );

			_theEntity.lastName = "Smith";
		} );
	}

	@Entity( name = "TheEntity" )
	private static class TheEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@Generated
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Date createdDate;

		@Generated(event = { EventType.INSERT, EventType.UPDATE})
		@ColumnDefault( "CURRENT_TIMESTAMP" )
		@Column( nullable = false )
		private Calendar alwaysDate;

		@CreationTimestamp
		private Date vmCreatedDate;

		@CreationTimestamp
		private Calendar vmCreatedCalendar;

		@CreationTimestamp
		private java.sql.Date vmCreatedSqlDate;

		@CreationTimestamp
		private Time vmCreatedSqlTime;

		@CreationTimestamp
		private Timestamp vmCreatedSqlTimestamp;

		@CreationTimestamp
		private Instant vmCreatedSqlInstant;

		@CreationTimestamp
		private LocalDate vmCreatedSqlLocalDate;

		@CreationTimestamp
		private LocalTime vmCreatedSqlLocalTime;

		@CreationTimestamp
		private LocalDateTime vmCreatedSqlLocalDateTime;

		@CreationTimestamp
		private MonthDay vmCreatedSqlMonthDay;

		@CreationTimestamp
		private OffsetDateTime vmCreatedSqlOffsetDateTime;

		@CreationTimestamp
		private OffsetTime vmCreatedSqlOffsetTime;

		@CreationTimestamp
		private Year vmCreatedSqlYear;

		@CreationTimestamp
		private YearMonth vmCreatedSqlYearMonth;

		@CreationTimestamp
		private ZonedDateTime vmCreatedSqlZonedDateTime;

		@FunctionCreationTimestamp
		private Date dbCreatedDate;

		@UpdateTimestamp
		private Timestamp updated;

		@StaticGeneration
		private String name;

		@SuppressWarnings("unused")
		private String lastName;

		private TheEntity() {
		}

		private TheEntity(Integer id) {
			this.id = id;
		}
	}

	@ValueGenerationType(generatedBy = FunctionCreationValueGeneration.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FunctionCreationTimestamp {
	}

	public static class FunctionCreationValueGeneration implements OnExecutionGenerator {
		@Override
		public EnumSet<EventType> getEventTypes() {
			return EnumSet.of( EventType.INSERT );
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { "current_timestamp" };
		}
	}

}
