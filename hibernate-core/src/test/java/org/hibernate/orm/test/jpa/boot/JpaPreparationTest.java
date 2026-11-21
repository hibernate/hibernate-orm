package org.hibernate.orm.test.jpa.boot;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.boot.pipeline.internal.BootstrapPipeline;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.type.BasicType;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CLASSLOADERS;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

/// Verifies declared resources, converter discovery, and archive boundaries in the bootstrap pipeline.
///
/// @author Steve Ebersole
@BaseUnitTest
public class JpaPreparationTest {
	private static boolean jarDefaultUseCaches;
	@TempDir Path directory;

	@BeforeAll
	static void setup() {
		jarDefaultUseCaches = URLConnection.getDefaultUseCaches( "jar" );
		URLConnection.setDefaultUseCaches( "jar", false );
	}

	@AfterAll
	static void cleanup() {
		URLConnection.setDefaultUseCaches( "jar", jarDefaultUseCaches );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void resolvesDeclaredClassesAndAutoApplyConverters(boolean hibernate) {
		final PersistenceConfiguration configuration = hibernate
				? new HibernatePersistenceConfiguration( "preparation" ) : new PersistenceConfiguration( "preparation" );
		configure( configuration, getClass().getClassLoader() );
		try ( var result = BootstrapPipeline.resolveMetadata( configuration ) ) {
			final var metadata = result.metadata();
			assertThat( metadata.getEntityBinding( LoadedEntity.class.getName() ) ).isNotNull();
			final var type = (BasicType<?>) metadata.getEntityBinding( DeclaredEntity.class.getName() ).getProperty( "value" ).getType();
			assertThat( type.getValueConverter() ).isNotNull();
			assertThat( type.getValueConverter().getDomainJavaType().getJavaTypeClass() ).isEqualTo( Value.class );
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
			final PersistenceConfiguration selected = hibernateConfiguration ? configuration
					: configure( new PersistenceConfiguration( "preparation" ), loader ).property( XML_MAPPING_ENABLED, xmlEnabled );
			try ( var result = BootstrapPipeline.resolveMetadata( selected ) ) {
				final var metadata = result.metadata();
				assertThat( metadata.getEntityBinding( DeclaredEntity.class.getName() ) ).isNotNull();
				assertThat( metadata.getEntityBinding( DiscoveredEntity.class.getName() ) != null ).isEqualTo( hibernateConfiguration );
				assertThat( metadata.getEntityBinding( XmlEntity.class.getName() ) != null ).isEqualTo( hibernateConfiguration && xmlEnabled );
			}
		}
	}

	private static PersistenceConfiguration configure(PersistenceConfiguration configuration, ClassLoader loader) {
		return configuration.managedClass( DeclaredEntity.class ).managedClass( LoadedEntity.class ).managedClass( ValueConverter.class )
				.property( CLASSLOADERS, List.of( loader ) ).property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false );
	}

	private URLClassLoader contributorLoader() throws Exception {
		return new URLClassLoader( new URL[] { directory.toUri().toURL() }, getClass().getClassLoader() );
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
