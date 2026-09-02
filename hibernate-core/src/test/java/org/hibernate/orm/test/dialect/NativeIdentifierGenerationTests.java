/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.Map;

import jakarta.persistence.GenerationType;

import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupportBase;
import org.hibernate.generator.Generator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.mapping.SimpleValue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies typed native identifier-generation selection and its legacy
/// `"native"` bootstrap consumers.
///
/// @author Steve Ebersole
public class NativeIdentifierGenerationTests {
	private static final Map<GenerationType, Class<? extends Generator>> NATIVE_GENERATORS = Map.of(
			GenerationType.IDENTITY, IdentityGenerator.class,
			GenerationType.SEQUENCE, SequenceStyleGenerator.class,
			GenerationType.AUTO, SequenceStyleGenerator.class,
			GenerationType.TABLE, org.hibernate.id.enhanced.TableGenerator.class,
			GenerationType.UUID, UUIDGenerator.class
	);

	@Test
	void bothLegacyConsumersMapEveryTypedNativeValueDirectly() {
		for ( var entry : NATIVE_GENERATORS.entrySet() ) {
			final Dialect dialect = dialectReturning( entry.getKey() );
			assertThat( GeneratorStrategies.mapLegacyNamedGenerator( "native", dialect ) )
					.as( "named native mapping for " + entry.getKey() )
					.isEqualTo( entry.getValue() );
			assertThat( GeneratorStrategies.generatorClass( "native", idValue( dialect ) ) )
					.as( "generator class native mapping for " + entry.getKey() )
					.isEqualTo( entry.getValue() );
		}
	}

	@Test
	void inheritedDefaultUsesIdentitySupport() {
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.SEQUENCE );

		final IdentityColumnSupport identitySupport = new IdentityColumnSupportBase() {
			@Override
			public boolean supportsIdentityColumns() {
				return true;
			}
		};
		assertThat( new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public IdentityColumnSupport getIdentityColumnSupport() {
				return identitySupport;
			}
		}.getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.IDENTITY );
	}

	@Test
	void maintainedDialectsPreserveTheirTypedValues() {
		assertThat( new PostgreSQLDialect().getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.SEQUENCE );
		assertThat( new CockroachDialect().getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.SEQUENCE );
		assertThat( new OracleDialect().getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.SEQUENCE );
		assertThat( new SpannerDialect().getNativeValueGenerationStrategy() ).isEqualTo( GenerationType.SEQUENCE );
	}

	@Test
	void nullProviderAnswerIsRejectedByBothLegacyConsumers() {
		final Dialect dialect = dialectReturning( null );
		assertThatNullPointerException()
				.isThrownBy( () -> GeneratorStrategies.mapLegacyNamedGenerator( "native", dialect ) )
				.withMessage( "Dialect#getNativeValueGenerationStrategy() returned null" );
		assertThatNullPointerException()
				.isThrownBy( () -> GeneratorStrategies.generatorClass( "native", idValue( dialect ) ) )
				.withMessage( "Dialect#getNativeValueGenerationStrategy() returned null" );
	}

	@Test
	void explicitLegacyAliasesAndUnknownNamesKeepTheirBehavior() {
		assertThat( GeneratorStrategies.mapLegacyNamedGenerator( "uuid", baseDialect() ) )
				.isEqualTo( UUIDHexGenerator.class );
		assertThat( GeneratorStrategies.mapLegacyNamedGenerator( "uuid.hex", baseDialect() ) )
				.isEqualTo( UUIDHexGenerator.class );
		assertThat( GeneratorStrategies.mapLegacyNamedGenerator( "uuid2", baseDialect() ) )
				.isEqualTo( UUIDGenerator.class );
		assertThat( GeneratorStrategies.mapLegacyNamedGenerator( "unknown", baseDialect() ) ).isNull();

		assertThat( GeneratorStrategies.generatorClass( "uuid", null ) ).isEqualTo( UUIDHexGenerator.class );
		assertThat( GeneratorStrategies.generatorClass( "uuid.hex", null ) ).isEqualTo( UUIDHexGenerator.class );
		assertThat( GeneratorStrategies.generatorClass( "uuid2", null ) ).isEqualTo( UUIDGenerator.class );

		final SimpleValue idValue = idValue( baseDialect() );
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( idValue.getBuildingContext().getBootstrapContext().getClassLoaderService() )
				.thenReturn( classLoaderService );
		doReturn( IncrementGenerator.class )
				.when( classLoaderService )
				.classForName( IncrementGenerator.class.getName() );
		assertThat( GeneratorStrategies.generatorClass( IncrementGenerator.class.getName(), idValue ) )
				.isEqualTo( IncrementGenerator.class );
	}

	private static Dialect dialectReturning(GenerationType generationType) {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public GenerationType getNativeValueGenerationStrategy() {
				return generationType;
			}
		};
	}

	private static Dialect baseDialect() {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
		};
	}

	private static SimpleValue idValue(Dialect dialect) {
		final SimpleValue idValue = mock( SimpleValue.class, RETURNS_DEEP_STUBS );
		when( idValue.getMetadata().getDatabase().getDialect() ).thenReturn( dialect );
		return idValue;
	}
}
