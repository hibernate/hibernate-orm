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
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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
			GenerationType.UUID, UuidGenerator.class
	);

	@Test
	void nativeStrategyMapsEveryTypedValueDirectly() {
		for ( var entry : NATIVE_GENERATORS.entrySet() ) {
			final Dialect dialect = dialectReturning( entry.getKey() );
			assertThat( GeneratorStrategies.resolveLegacyGeneratorClass( "native", dialect ) )
					.as( "named native mapping for " + entry.getKey() )
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
				.isThrownBy( () -> GeneratorStrategies.resolveLegacyGeneratorClass( "native", dialect ) )
				.withMessage( "Dialect#getNativeValueGenerationStrategy() returned null" );
	}

	@Test
	void removedUuidAliasesAreNotResolvedAndClassNamesStillWork() {
		for ( var alias : java.util.List.of( "uuid", "uuid.hex", "uuid2", "unknown" ) ) {
			assertThat( GeneratorStrategies.resolveLegacyGeneratorClass( alias, baseDialect() ) ).isNull();
		}
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		doReturn( IncrementGenerator.class ).when( classLoaderService ).classForName( IncrementGenerator.class.getName() );
		assertThat( GeneratorStrategies.resolveGeneratorClass( IncrementGenerator.class.getName(), baseDialect(), classLoaderService ) )
				.isEqualTo( IncrementGenerator.class );
	}

	@Test
	void nativeUuidStrategyBuildsTheModernGenerator() {
		final var configuration = new org.hibernate.cfg.Configuration();
		configuration.getProperties().put( org.hibernate.cfg.AvailableSettings.DIALECT, new org.hibernate.dialect.H2Dialect() {
			@Override
			public GenerationType getNativeValueGenerationStrategy() {
				return GenerationType.UUID;
			}
		} );
		configuration.setProperty( org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT, "false" );
		configuration.addAnnotatedClass( NativeUuidEntity.class );
		final String mapping = """
				<entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping" version="9.0">
					<entity class="org.hibernate.orm.test.dialect.NativeIdentifierGenerationTests$NativeUuidEntity">
						<attributes>
							<id name="id">
								<generated-value generator="native-id"/>
								<generic-generator name="native-id" class="native"/>
							</id>
						</attributes>
					</entity>
				</entity-mappings>
				""";
		configuration.addInputStream( new java.io.ByteArrayInputStream( mapping.getBytes( java.nio.charset.StandardCharsets.UTF_8 ) ) );
		try ( var factory = configuration.buildSessionFactory() ) {
			final var entity = factory.unwrap( org.hibernate.engine.spi.SessionFactoryImplementor.class )
					.getMappingMetamodel().getEntityDescriptor( NativeUuidEntity.class );
			assertThat( entity.getGenerator() ).isInstanceOfSatisfying(
					org.hibernate.id.GenericGeneratorGeneration.class,
					generator -> assertThat( generator.getDelegate() ).isInstanceOf( UuidGenerator.class )
			);
		}
	}

	@jakarta.persistence.Entity
	public static class NativeUuidEntity {
		@jakarta.persistence.Id
		public java.util.UUID id;
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

}
