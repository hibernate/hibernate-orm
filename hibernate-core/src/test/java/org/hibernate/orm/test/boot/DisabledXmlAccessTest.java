package org.hibernate.orm.test.boot;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;

import jakarta.persistence.FetchType;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.pipeline.internal.source.MappingSourcePreparationContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.pipeline.internal.source.PreparedMappingSources;
import org.hibernate.boot.pipeline.internal.source.XmlMappingSource;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.XML_MAPPING_ENABLED;

/// Disabled mapping XML must not access resources or consume caller-owned streams.
///
/// @author Steve Ebersole
@BaseUnitTest
class DisabledXmlAccessTest {
	@Test
	void disabledResourceIsNotLookedUp() {
		final var loader = new ClassLoader( getClass().getClassLoader() ) {
			@Override
			public URL getResource(String name) {
				if ( name.equals( "missing-disabled.xml" ) ) {
					throw new AssertionError( "Looked up disabled resource" );
				}
				return super.getResource( name );
			}
		};
		try ( var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( loader ).build();
				var registry = new StandardServiceRegistryBuilder( bootstrap ).build() ) {
			assertThat( prepare( new MappingSources().addMappingResource( "missing-disabled.xml" ), registry, false ).xmlMappings() ).isEmpty();
		}
	}

	@Test
	void sourcesSkipEverySupportedXmlInputForm() throws Exception {
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			final var file = inaccessibleFile();
			final var sources = new MappingSources()
					.addManagedClass( String.class ).addPackage( "org.hibernate.orm.test.boot.models.source.registrationtargets" )
					.addMappingResource( "missing-disabled.xml" ).addMappingFile( file )
					.addMappingUrl( inaccessibleUrl() )
					.addXmlMappingSource( XmlMappingSource.fromCacheableFile( file, file, false ) )
					.addXmlMappingSource( XmlMappingSource.fromCacheableFile( file, file, true ) )
					.addXmlMappingSource( XmlMappingSource.fromInputStream( inaccessibleStream() ) )
					.addXmlMappingSource( XmlMappingSource.fromInputStreamAccess( new InputStreamAccess() {
						@Override public String getStreamName() { return "disabled"; }
						@Override public InputStream accessInputStream() {
							throw new AssertionError( "Acquired disabled stream" );
						}
					} ) );
			final var prepared = prepare( sources, registry, false );
			assertThat( prepared.xmlMappings() ).isEmpty();
			assertThat( prepared.managedClassDetails() ).extracting( detail -> detail.getName() ).containsExactly( String.class.getName() );
			assertThat( prepared.packageDetails() ).hasSize( 1 );
		}
	}

	@Test
	void preparationUsesCurrentSettingsAndDefersResourceAccess() {
		final var sources = new MappingSources().addMappingResource( "missing-enabled.xml" );
		try ( var registry = new StandardServiceRegistryBuilder().build() ) {
			assertThat( prepare( sources, registry, false ).xmlMappings() ).isEmpty();
			assertThatThrownBy( () -> prepare( sources, registry, true ) ).hasMessageContaining( "missing-enabled.xml" );
		}
	}

	private static PreparedMappingSources prepare(MappingSources sources, StandardServiceRegistry registry, boolean enabled) {
		final var context = new MetadataBuildingContextTestingImpl( registry );
		return PreparedMappingSources.from( sources, new MappingSourcePreparationContext( context.getModelsContext(), registry ),
				SettingsResolver.resolveMappingSettings( SettingsResolver.resolveBootstrapSettings( Map.of( XML_MAPPING_ENABLED, enabled ) ), FetchType.EAGER ) );
	}

	private static InputStream inaccessibleStream() {
		return new InputStream() {
			@Override
			public int read() {
				throw new AssertionError( "Read disabled stream" );
			}

			@Override
			public void close() {
				throw new AssertionError( "Closed caller-owned stream" );
			}
		};
	}

	private static URL inaccessibleUrl() throws Exception {
		return new URL( null, "test:disabled", new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL url) {
				throw new AssertionError( "Opened disabled URL" );
			}
		} );
	}

	private static File inaccessibleFile() {
		return new File( "disabled.xml" ) {
			@Override
			public boolean exists() {
				throw new AssertionError( "Accessed disabled file" );
			}

			@Override
			public long lastModified() {
				throw new AssertionError( "Accessed disabled cache" );
			}

			@Override
			public File[] listFiles() {
				throw new AssertionError( "Traversed disabled directory" );
			}
		};
	}
}
