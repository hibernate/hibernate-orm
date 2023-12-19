/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.temporal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
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
	@DomainModel(annotatedClasses = TestEntity.class)
	void testMapping(DomainModelScope scope) {
		final MetadataImplementor domainModel = scope.getDomainModel();
		final PersistentClass entityBinding = scope.getEntityBinding( TestEntity.class );

		final Dialect dialect = domainModel.getDatabase().getDialect();
		final int defaultPrecision = dialect.getDefaultTimestampPrecision();

		checkPrecision( "theInstant", 3, entityBinding, domainModel );
		checkPrecision( "theInstant2", defaultPrecision, entityBinding, domainModel );

		checkPrecision( "theLocalDateTime", 3, entityBinding, domainModel );
		checkPrecision( "theLocalDateTime2", defaultPrecision, entityBinding, domainModel );

		checkPrecision( "theLocalTime", 3, entityBinding, domainModel );
		checkPrecision( "theLocalTime2", defaultPrecision, entityBinding, domainModel );

		checkPrecision( "theOffsetDateTime", 9, entityBinding, domainModel );
		checkPrecision( "theOffsetDateTime2", defaultPrecision, entityBinding, domainModel );

		checkPrecision( "theOffsetTime", 9, entityBinding, domainModel );
		checkPrecision( "theOffsetTime2", defaultPrecision, entityBinding, domainModel );

		checkPrecision( "theZonedDateTime", 9, entityBinding, domainModel );
		checkPrecision( "theZonedDateTime2", defaultPrecision, entityBinding, domainModel );
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
	void testBasicUsage(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final TestEntity testEntity = new TestEntity();
			testEntity.id = 1;
			testEntity.theInstant = Instant.now();
			session.persist( testEntity );
		} );

		scope.inTransaction( (session) -> {
			final TestEntity testEntity = session.find( TestEntity.class, 1 );

			assertThat( testEntity.theInstant ).isEqualTo( DateTimeUtils.roundToSecondPrecision( testEntity.theInstant, 3 ) );
		} );
	}

	@Entity(name="TestEntity")
	@Table(name="TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@FractionalSeconds(3)
		private Instant theInstant;
		private Instant theInstant2;

		@FractionalSeconds(3)
		private LocalDateTime theLocalDateTime;
		private LocalDateTime theLocalDateTime2;

		@FractionalSeconds(3)
		private LocalTime theLocalTime;
		private LocalTime theLocalTime2;

		@FractionalSeconds(9)
		private OffsetDateTime theOffsetDateTime;
		private OffsetDateTime theOffsetDateTime2;

		@FractionalSeconds(9)
		private OffsetTime theOffsetTime;
		private OffsetTime theOffsetTime2;

		@FractionalSeconds(9)
		private ZonedDateTime theZonedDateTime;
		private ZonedDateTime theZonedDateTime2;

	}
}
