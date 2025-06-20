/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.rfc9562;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.id.uuid.UuidVersion6Strategy;
import org.hibernate.id.uuid.UuidVersion7Strategy;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.uuid.IdGeneratorCreationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		EntitySeven.class, OtherEntitySeven.class, EntitySix.class
})
@SessionFactory
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
public class UuidGeneratorAnnotationTests {
	@Test
	public void verifyUuidV7IdGeneratorModel(final DomainModelScope scope) {
		scope.withHierarchy( EntitySeven.class, descriptor -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();
			final Generator generator = value.getCustomIdGeneratorCreator()
					.createGenerator( new IdGeneratorCreationContext(
							scope.getDomainModel(),
							descriptor
					) );

			assertThat( generator ).isInstanceOf( UuidGenerator.class );
			final UuidGenerator uuidGenerator = (UuidGenerator) generator;
			assertThat( uuidGenerator.getValueGenerator() ).isInstanceOf( UuidVersion7Strategy.class );
		} );
	}

	@Test
	public void verifyUuidV6IdGeneratorModel(final DomainModelScope scope) {
		scope.withHierarchy( EntitySix.class, descriptor -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();
			final Generator generator = value.getCustomIdGeneratorCreator()
					.createGenerator( new IdGeneratorCreationContext(
							scope.getDomainModel(),
							descriptor
					) );

			assertThat( generator ).isInstanceOf( UuidGenerator.class );
			final UuidGenerator uuidGenerator = (UuidGenerator) generator;
			assertThat( uuidGenerator.getValueGenerator() ).isInstanceOf( UuidVersion6Strategy.class );
		} );
	}

	@Test
	public void basicUseTest(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntitySeven seven = new EntitySeven( "John Doe" );
			session.persist( seven );
			session.flush();
			assertThat( seven.id ).isNotNull();
			assertThat( seven.id.version() ).isEqualTo( 7 );
		} );
	}

	@Test
	public void nonPkUseTest(final SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Instant startTime = Instant.now();

			final OtherEntitySeven seven = new OtherEntitySeven( "Dave Default" );
			session.persist( seven );
			session.flush();

			final Instant endTime = Instant.now();
			assertThat( seven.id ).isNotNull();
			assertThat( seven.id.version() ).isEqualTo( 7 );

			assertThat( Instant.ofEpochMilli( seven.id.getMostSignificantBits() >> 16 & 0xFFFF_FFFF_FFFFL ) )
					.isBetween( startTime.truncatedTo( ChronoUnit.MILLIS ), endTime.truncatedTo( ChronoUnit.MILLIS ) );
		} );
	}

	@Test
	void testUuidV6IdGenerator(final SessionFactoryScope sessionFactoryScope) {
		sessionFactoryScope.inTransaction( session -> {
			final Instant startTime = Instant.now();

			final EntitySix six = new EntitySix( "Jane Doe" );
			session.persist( six );
			assertThat( six.getId() ).isNotNull();
			assertThat( six.getId().version() ).isEqualTo( 6 );

			session.flush();
			final Instant endTime = Instant.now();
			assertThat( six.getId() ).isNotNull();
			assertThat( six.getId().version() ).isEqualTo( 6 );
			assertThat( uuid6Instant( six.getId() ) ).isBetween( startTime, endTime );
		} );
	}

	@AfterEach
	void dropTestData(final SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static Instant uuid6Instant(final UUID uuid) {
		assert uuid.version() == 6;

		final var msb = uuid.getMostSignificantBits();
		final var ts = msb >> 4 & 0x0FFF_FFFF_FFFF_F000L | msb & 0x0FFFL;
		return LocalDate.of( 1582, 10, 15 ).atStartOfDay( ZoneId.of( "UTC" ) ).toInstant()
				.plusSeconds( ts / 10_000_000 ).plusNanos( ts % 10_000_000 * 100 );
	}

}
