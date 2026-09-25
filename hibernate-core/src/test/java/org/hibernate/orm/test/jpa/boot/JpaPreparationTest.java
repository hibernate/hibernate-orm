/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.internal.NamedBasicTypeImpl;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.LOADED_CLASSES;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;
import static org.hibernate.jpa.boot.spi.JpaSettings.METADATA_BUILDER_CONTRIBUTOR;

/// Characterizes preparation state and extension ordering in both JPA builder constructors.
///
/// @author Steve Ebersole
@BaseUnitTest
public class JpaPreparationTest {
	private static final ThreadLocal<State> STATE = ThreadLocal.withInitial( State::new );
	private static final String TYPE_NAME = "preparation_type";
	private static final NamedBasicTypeImpl<String> CONFIGURED_TYPE =
			new NamedBasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE, "configured" );
	private static final NamedBasicTypeImpl<String> SERVICE_TYPE =
			new NamedBasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE, "service" );

	@TempDir
	Path directory;

	@AfterEach
	void clearState() {
		STATE.remove();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void preparesDeclaredAndLoadedResourcesBeforeResolvingMetadata(boolean hibernateConfiguration) throws Exception {
		try ( var loader = contributorLoader() ) {
			final var configuration = configuration( hibernateConfiguration, loader );
			final var builder = builder( configuration );
			try {
				assertThat( STATE.get().events ).containsExactly( "configured", "service" );
				assertThat( STATE.get().registryHook ).isEqualTo( !hibernateConfiguration );
				assertThat( builder.getMetadata() ).isNull();
				assertThat( builder.getManagedResources().getAnnotatedClassReferences() )
						.contains( DeclaredEntity.class, LoadedEntity.class );
				assertThat( builder.getManagedResources().getAttributeConverterDescriptors() )
						.extracting( descriptor -> descriptor.getAttributeConverterClass().getName() )
						.contains( ValueConverter.class.getName() );
				final var metadata = builder.metadata();
				assertThat( metadata.getEntityBinding( LoadedEntity.class.getName() ) ).isNotNull();
				final var type = (org.hibernate.type.BasicType<?>) metadata.getEntityBinding( DeclaredEntity.class.getName() )
						.getProperty( "value" ).getType();
				assertThat( type.getValueConverter() ).isNotNull();
				assertThat( metadata.getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( TYPE_NAME ) )
						.isSameAs( SERVICE_TYPE );
				assertThat( builder.metadata() ).isSameAs( metadata );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@Test
	void enhancementSeesPreparedResourcesAndTemporaryClassLoader() throws Exception {
		try ( var loader = contributorLoader(); var temporaryLoader = new URLClassLoader( new URL[0], loader ) ) {
			STATE.get().temporaryLoader = temporaryLoader;
			final var descriptor = new PersistenceConfigurationDescriptor( configuration( false, loader ) ) {
				@Override
				public ClassLoader getTempClassLoader() {
					return temporaryLoader;
				}

				@Override
				public boolean isClassTransformerRegistrationDisabled() {
					return false;
				}
			};
			final var builder = new TrackingBuilder( descriptor );
			try {
				assertThat( STATE.get().events ).containsExactly( "configured", "service", "enhancement" );
				assertThat( STATE.get().metadataBuilder.getBootstrapContext().getJpaTempClassLoader() ).isNull();
				assertThat( builder.getMetadata() ).isNull();
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "false,true", "true,false", "true,true" })
	void onlyHibernateConfigurationAddsArchiveDiscoveries(boolean hibernateConfiguration, boolean xmlEnabled) throws Exception {
		final var archiveFile = directory.resolve( "discovered.jar" ).toFile();
		ShrinkWrap.create( JavaArchive.class )
				.addClass( DiscoveredEntity.class )
				.addAsManifestResource( new StringAsset( """
						<entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm" version="3.2">
							<entity class="%s" access="FIELD">
								<attributes><id name="id"/></attributes>
							</entity>
						</entity-mappings>
						""".formatted( XmlEntity.class.getName() ) ), "orm.xml" )
				.as( ZipExporter.class ).exportTo( archiveFile );
		try ( var loader = contributorLoader() ) {
			final var configuration = configure(
					new HibernatePersistenceConfiguration( "preparation", archiveFile.toURI().toURL() ), loader )
					.property( XML_MAPPING_ENABLED, xmlEnabled );
			// The same declarations and archive are supplied to each constructor.
			final var builder = hibernateConfiguration
					? new TrackingBuilder( (HibernatePersistenceConfiguration) configuration )
					: new TrackingBuilder( new PersistenceConfigurationDescriptor( configuration ) );
			try {
				assertThat( builder.getManagedResources().getXmlMappingBindings() )
						.hasSize( hibernateConfiguration && xmlEnabled ? 1 : 0 );
				final var metadata = builder.metadata();
				assertThat( metadata.getEntityBinding( DeclaredEntity.class.getName() ) ).isNotNull();
				assertThat( metadata.getEntityBinding( DiscoveredEntity.class.getName() ) != null )
						.isEqualTo( hibernateConfiguration );
				assertThat( metadata.getEntityBinding( XmlEntity.class.getName() ) != null )
						.isEqualTo( hibernateConfiguration && xmlEnabled );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void contributorFailurePropagatesAndClosesBootstrapRegistry(boolean hibernateConfiguration) throws Exception {
		try ( var loader = contributorLoader() ) {
			final var failure = new IllegalStateException( "preparation failed" );
			final var configuration = configuration( hibernateConfiguration, loader );
			configuration.property( METADATA_BUILDER_CONTRIBUTOR, (MetadataBuilderContributor) metadataBuilder -> {
				STATE.get().metadataBuilder = (MetadataBuilderImplementor) metadataBuilder;
				throw failure;
			} );
			assertThatThrownBy( () -> builder( configuration ) ).isSameAs( failure );
			final var registry = STATE.get().metadataBuilder.getBootstrapContext().getServiceRegistry();
			try {
				assertThat( ((org.hibernate.boot.registry.internal.BootstrapServiceRegistryImpl)
						registry.getParentServiceRegistry()).isActive() ).isFalse();
				assertThat( STATE.get().events ).isEmpty();
			}
			finally {
				StandardServiceRegistryBuilder.destroy( registry );
			}
		}
	}

	private static PersistenceConfiguration configuration(boolean hibernateConfiguration, ClassLoader loader) {
		final PersistenceConfiguration configuration = hibernateConfiguration
				? new HibernatePersistenceConfiguration( "preparation" )
				: new PersistenceConfiguration( "preparation" );
		return configure( configuration, loader );
	}

	private static PersistenceConfiguration configure(PersistenceConfiguration configuration, ClassLoader loader) {
		return configuration.managedClass( DeclaredEntity.class )
				.property( CLASSLOADERS, List.of( loader ) )
				.property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false )
				.property( LOADED_CLASSES, List.of( LoadedEntity.class, ValueConverter.class ) )
				.property( METADATA_BUILDER_CONTRIBUTOR, (MetadataBuilderContributor) metadataBuilder -> {
					STATE.get().events.add( "configured" );
					STATE.get().metadataBuilder = (MetadataBuilderImplementor) metadataBuilder;
					assertThat( STATE.get().metadataBuilder.getBootstrapContext().getJpaTempClassLoader() )
							.isSameAs( STATE.get().temporaryLoader );
					metadataBuilder.applyBasicType( CONFIGURED_TYPE, TYPE_NAME );
				} );
	}

	private static TrackingBuilder builder(PersistenceConfiguration configuration) {
		return configuration instanceof HibernatePersistenceConfiguration hibernateConfiguration
				? new TrackingBuilder( hibernateConfiguration )
				: new TrackingBuilder( new PersistenceConfigurationDescriptor( configuration ) );
	}

	private URLClassLoader contributorLoader() throws Exception {
		final var services = Files.createDirectories( directory.resolve( "META-INF/services" ) );
		Files.writeString( services.resolve( MetadataBuilderContributor.class.getName() ), ServiceContributor.class.getName() );
		return new URLClassLoader( new URL[] { directory.toUri().toURL() }, getClass().getClassLoader() );
	}

	private static class TrackingBuilder extends EntityManagerFactoryBuilderImpl {
		TrackingBuilder(HibernatePersistenceConfiguration configuration) {
			super( configuration );
		}

		TrackingBuilder(PersistenceConfigurationDescriptor descriptor) {
			super( descriptor, Map.of() );
		}

		@Override
		protected StandardServiceRegistryBuilder getStandardServiceRegistryBuilder(BootstrapServiceRegistry registry) {
			STATE.get().registryHook = true;
			return super.getStandardServiceRegistryBuilder( registry );
		}

		@Override
		protected EnhancementModel getEnhancementModel() {
			assertThat( getManagedResources().getAnnotatedClassReferences() ).contains( DeclaredEntity.class, LoadedEntity.class );
			assertThat( STATE.get().metadataBuilder.getBootstrapContext().getJpaTempClassLoader() )
					.isSameAs( STATE.get().temporaryLoader );
			STATE.get().events.add( "enhancement" );
			return super.getEnhancementModel();
		}
	}

	public static class ServiceContributor implements MetadataBuilderContributor {
		@Override
		public void contribute(MetadataBuilder metadataBuilder) {
			assertThat( STATE.get().events ).containsExactly( "configured" );
			STATE.get().events.add( "service" );
			metadataBuilder.applyBasicType( SERVICE_TYPE, TYPE_NAME );
		}
	}

	private static class State {
		final List<String> events = new ArrayList<>();
		MetadataBuilderImplementor metadataBuilder;
		ClassLoader temporaryLoader;
		boolean registryHook;
	}

	public record Value(String text) {
	}

	@Converter(autoApply = true)
	public static class ValueConverter implements AttributeConverter<Value, String> {
		@Override
		public String convertToDatabaseColumn(Value value) {
			return value == null ? null : value.text();
		}

		@Override
		public Value convertToEntityAttribute(String value) {
			return value == null ? null : new Value( value );
		}
	}

	@Entity
	public static class DeclaredEntity {
		@Id
		Long id;
		Value value;
	}

	@Entity
	public static class LoadedEntity {
		@Id
		Long id;
	}

	@Entity
	public static class DiscoveredEntity {
		@Id
		Long id;
	}

	public static class XmlEntity {
		Long id;
	}
}
