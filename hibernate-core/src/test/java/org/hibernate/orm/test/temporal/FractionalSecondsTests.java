/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.temporal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.descriptor.DateTimeUtils;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class FractionalSecondsTests {
	@Test
	@DomainModel(annotatedClasses = {TestEntity.class, TestEntity0.class, TestEntity3.class, TestEntity9.class} )
	void testMapping(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();

		final Dialect dialect = domainModel.getDatabase().getDialect();
		final int defaultPrecision = dialect.getDefaultTimestampPrecision();

		final PersistentClass entityBinding = scope.getEntityBinding( TestEntity.class );
		checkPrecision( "theInstant", defaultPrecision, entityBinding, domainModel );
		checkPrecision( "theLocalDateTime", defaultPrecision, entityBinding, domainModel );
		checkPrecision( "theLocalTime", 0, entityBinding, domainModel );
		checkPrecision( "theOffsetDateTime", defaultPrecision, entityBinding, domainModel );
		checkPrecision( "theOffsetTime", 0, entityBinding, domainModel );
		checkPrecision( "theZonedDateTime", defaultPrecision, entityBinding, domainModel );

		final PersistentClass entityBinding0 = scope.getEntityBinding( TestEntity0.class );
		checkPrecision( "theInstant", 0, entityBinding0, domainModel );

		final PersistentClass entityBinding3 = scope.getEntityBinding( TestEntity3.class );
		checkPrecision( "theInstant", 3, entityBinding3, domainModel );
		checkPrecision( "theLocalDateTime", 3, entityBinding3, domainModel );
		checkPrecision( "theLocalTime", 3, entityBinding3, domainModel );

		final PersistentClass entityBinding9 = scope.getEntityBinding( TestEntity9.class );
		checkPrecision( "theInstant", 9, entityBinding9, domainModel );
		checkPrecision( "theOffsetDateTime", 9, entityBinding9, domainModel );
		checkPrecision( "theOffsetTime", 9, entityBinding9, domainModel );
		checkPrecision( "theZonedDateTime", 9, entityBinding9, domainModel );
	}

	private void checkPrecision(
			String propertyName,
			int expectedMinimumSize,
			PersistentClass entityBinding,
			MetadataImplementor domainModel) {
		final Property theInstant = entityBinding.getProperty( propertyName );
		final BasicValue value = (BasicValue) theInstant.getValue();
		final Column column = (Column) value.getColumn();
		final Size columnSize = column.getColumnSize( value.getDialect(), domainModel );
		assertThat( columnSize.getPrecision() ).isEqualTo( expectedMinimumSize );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity.class)
	@SessionFactory
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	void testUsage(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getDefaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity testEntity = session.find( TestEntity.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToDefaultPrecision( start, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity0.class)
	@SessionFactory
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby does not support sized timestamp")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix only supports precision from 1 to 5")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird does not support specifying a precision on timestamps")
	void testUsage0(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getDefaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity0 testEntity = new TestEntity0();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity0 testEntity = session.find( TestEntity0.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToPrecision( start, 0, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity3.class)
	@SessionFactory
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby does not support sized timestamp")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird does not support specifying a precision on timestamps")
	void testUsage3(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getDefaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity3 testEntity = new TestEntity3();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity3 testEntity = session.find( TestEntity3.class, 1 );
			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.adjustToPrecision( start, 3, dialect ) );
		} );
	}

	@Test
	@DomainModel(annotatedClasses = TestEntity9.class)
	@SessionFactory
	@SkipForDialect(dialectClass = MariaDBDialect.class, reason = "MariaDB only supports precision <= 6")
	@SkipForDialect(dialectClass = MySQLDialect.class, reason = "MySQL only supports precision <= 6", matchSubTypes = true)
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "SQL Server only supports precision <= 6")
	@SkipForDialect(dialectClass = SybaseDialect.class, reason = "Because... Sybase...", matchSubTypes = true)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, reason = "PostgreSQL only supports precision <= 6")
	@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "PostgresPlus only supports precision <= 6")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB only supports precision <= 6")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA does not support specifying a precision on timestamps")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix only supports precision from 1 to 5")
	@SkipForDialect(dialectClass = FirebirdDialect.class, reason = "Firebird only supports precision of 4")
	void testUsage9(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start;
		if ( dialect.getDefaultTimestampPrecision() == 6 ) {
			start = Instant.now().truncatedTo( ChronoUnit.MICROS );
		}
		else {
			start = Instant.now();
		}

		scope.inTransaction( (session) -> {
			final TestEntity9 testEntity = new TestEntity9();
			testEntity.id = 1;
			testEntity.theInstant = start;
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity9 testEntity = session.find( TestEntity9.class, 1 );

			assertThat( testEntity.theInstant ).isEqualTo( start );
		} );
	}

	@Entity(name="TestEntity")
	@Table(name="TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private Instant theInstant;

		private LocalDateTime theLocalDateTime;

		private LocalTime theLocalTime;

		private OffsetDateTime theOffsetDateTime;

		private OffsetTime theOffsetTime;

		private ZonedDateTime theZonedDateTime;

	}

	@Entity(name="TestEntity0")
	@Table(name="TestEntity0")
	public static class TestEntity0 {
		@Id
		private Integer id;

		@FractionalSeconds(0)
		private Instant theInstant;

	}

	@Entity(name="TestEntity3")
	@Table(name="TestEntity3")
	public static class TestEntity3 {
		@Id
		private Integer id;

		@FractionalSeconds(3)
		private Instant theInstant;

		@FractionalSeconds(3)
		private LocalDateTime theLocalDateTime;

		@FractionalSeconds(3)
		private LocalTime theLocalTime;

	}

	@Entity(name="TestEntity9")
	@Table(name="TestEntity9")
	public static class TestEntity9 {
		@Id
		private Integer id;

		@FractionalSeconds(9)
		private Instant theInstant;

		@FractionalSeconds(9)
		private OffsetDateTime theOffsetDateTime;

		@FractionalSeconds(9)
		private OffsetTime theOffsetTime;

		@FractionalSeconds(9)
		private ZonedDateTime theZonedDateTime;

	}
}
