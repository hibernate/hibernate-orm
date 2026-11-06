package org.hibernate.orm.test.any.keydefault;

import java.net.URLConnection;
import java.nio.file.Path;

import jakarta.persistence.Entity;
import org.hibernate.annotations.AnyKeyJavaType;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.any.keydefault.modulefixtures.plain.PlainOwner;
import org.hibernate.orm.test.any.keydefault.modulefixtures.string.StringOwner;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.module.TestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The key type of an {@link org.hibernate.annotations.Any} mapping defaults to the
 * {@link AnyKeyJavaType} declared on the module, unless specified by the package.
 *
 * @author Vincent Bouthinon
 */
@JiraKey("HHH-20319")
class AnyKeyModuleDefaultTest {
	@TempDir
	Path temporaryDirectory;

	// Ensure no JAR files are being cached to avoid file deletion issues on Windows
	private static boolean jarDefaultUseCaches;

	@BeforeAll
	public static void setup() {
		jarDefaultUseCaches = URLConnection.getDefaultUseCaches( "jar" );
		URLConnection.setDefaultUseCaches( "jar", false );
	}

	@AfterAll
	public static void cleanup() {
		URLConnection.setDefaultUseCaches( "jar", jarDefaultUseCaches );
	}

	@Test
	void moduleDefaultAndPackageOverride() throws Exception {
		final var archive = ShrinkWrap.create( JavaArchive.class, "any-key.jar" )
				.addPackage( PlainOwner.class.getPackage() )
				.addPackage( StringOwner.class.getPackage() );
		try (var module = TestModule.load( archive, """
				@org.hibernate.annotations.AnyKeyJavaType(org.hibernate.type.descriptor.java.LongJavaType.class)
				open module test.anykey {
				}
				""", temporaryDirectory, getClass().getClassLoader(),
				AnyKeyJavaType.class, Entity.class )) {
			final var plain = module.loadClass( PlainOwner.class.getName() );
			final var string = module.loadClass( StringOwner.class.getName() );
			final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( module.classLoader() ).build();
			try (var registry = new StandardServiceRegistryBuilder( bootstrap ).build()) {
				final var metadata = new MetadataSources( registry )
						.addAnnotatedClass( plain ).addAnnotatedClass( string ).buildMetadata();
				assertThat( keyJavaType( metadata, plain ) ).isEqualTo( Long.class );
				assertThat( keyJavaType( metadata, string ) ).isEqualTo( String.class );
			}
			finally {
				BootstrapServiceRegistryBuilder.destroy( bootstrap );
			}
		}
	}

	private static Class<?> keyJavaType(Metadata metadata, Class<?> owner) {
		final var content = (org.hibernate.mapping.Any)
				metadata.getEntityBinding( owner.getName() ).getProperty( "content" ).getValue();
		return content.getKeyDescriptor().resolve().getDomainJavaType().getJavaTypeClass();
	}
}
