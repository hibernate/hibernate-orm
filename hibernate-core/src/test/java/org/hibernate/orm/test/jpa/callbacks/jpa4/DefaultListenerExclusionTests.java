package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.orm.test.jpa.callbacks.jpa4.exclusion.DefaultListenerEntities;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.module.TestModule;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies default-listener exclusions with classpath and named-module mappings.
///
/// @author Steve Ebersole
@BaseUnitTest
@JiraKey("HHH-20991")
@JiraKey("HHH-20992")
class DefaultListenerExclusionTests {

	private static boolean jarDefaultUseCaches;

	@BeforeAll
	static void disableJarCaching() {
		jarDefaultUseCaches = URLConnection.getDefaultUseCaches( "jar" );
		URLConnection.setDefaultUseCaches( "jar", false );
	}

	@AfterAll
	static void restoreJarCaching() {
		URLConnection.setDefaultUseCaches( "jar", jarDefaultUseCaches );
	}

	static Stream<Arguments> scenarios() {
		return Stream.of( false, true ).flatMap( namedModule -> Stream.of( false, true )
				.flatMap( discovered -> Stream.of(
						Arguments.of( namedModule, "Plain", false, false, discovered ),
						Arguments.of( namedModule, "Excluded", true, false, discovered ),
						Arguments.of( namedModule, "Inherited", true, false, discovered ),
						Arguments.of( namedModule, "BothExcluded", true, true, discovered ),
						Arguments.of( namedModule, "SuperclassExcluded", false, true, discovered ),
						Arguments.of( namedModule, "InheritedWithSuperclassExcluded", true, true, discovered ),
						Arguments.of( namedModule, "XmlExcluded", true, false, discovered )
				) ) );
	}

	@ParameterizedTest(name = "module={0}, discovery={4}, entity={1}")
	@MethodSource("scenarios")
	void defaultListenerExclusion(
			boolean namedModule,
			String entityName,
			boolean excludeDefaults,
			boolean excludeSuperclass,
			boolean discovered,
			@TempDir Path directory) throws Exception {
		if ( namedModule ) {
			final var archive = ShrinkWrap.create( JavaArchive.class, "default-listeners.jar" )
					.addPackage( DefaultListenerEntities.class.getPackage() );
			try ( var module = TestModule.load( archive, """
					/// @author Steve Ebersole
					@jakarta.persistence.EntityListeners(
						org.hibernate.orm.test.jpa.callbacks.jpa4.exclusion.DefaultListenerEntities.ModuleListener.class)
					open module test.defaultlisteners {}
					""", directory, getClass().getClassLoader(), EntityListeners.class, DefaultListenerEntities.class ) ) {
				final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( module.classLoader() ).build();
				try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder( bootstrap )
						.applySetting( "hibernate.hbm2ddl.auto", "create-drop" ).build() ) {
					verify( registry, module.loadClass( DefaultListenerEntities.class.getName() ),
							entityName, excludeDefaults, excludeSuperclass, true, discovered );
				}
				finally {
					BootstrapServiceRegistryBuilder.destroy( bootstrap );
				}
			}
		}
		else {
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting( "hibernate.hbm2ddl.auto", "create-drop" ).build() ) {
				verify( registry, DefaultListenerEntities.class, entityName, excludeDefaults, excludeSuperclass, false, discovered );
			}
		}
	}

	private static void verify(
			StandardServiceRegistry registry,
			Class<?> fixtures,
			String entityName,
			boolean excludeDefaults,
			boolean excludeSuperclass,
			boolean namedModule,
			boolean discovered) throws Exception {
		final var sources = new MetadataSources( registry )
				.addPackageDescriptor( fixtures.getPackageName() )
				.addResource( "org/hibernate/orm/test/jpa/callbacks/jpa4/default-listener-exclusions.xml" );
		if ( discovered ) {
			sources.addAnnotatedClass( fixtures.getClassLoader().loadClass( fixtures.getName() + "$DiscoveredListener" ) );
		}
		Class<?> entityClass = null;
		for ( var type : fixtures.getDeclaredClasses() ) {
			if ( type.isAnnotationPresent( Entity.class ) ) {
				sources.addAnnotatedClass( type );
				if ( type.getSimpleName().equals( entityName ) ) {
					entityClass = type;
				}
			}
		}
		assertThat( entityClass ).isNotNull();
		assertThat( entityClass.getModule().isNamed() ).isEqualTo( namedModule );
		@SuppressWarnings("unchecked")
		final var events = (List<String>) fixtures.getField( "EVENTS" ).get( null );
		events.clear();
		try ( var factory = sources.buildMetadata().buildSessionFactory();
				var session = factory.openSession() ) {
			var entity = entityClass.getConstructor().newInstance();
			entityClass.getField( "id" ).setInt( entity, 1 );
			var transaction = session.beginTransaction();
			session.persist( entity );
			transaction.commit();
		}
		final var expected = new ArrayList<String>();
		if ( !excludeDefaults ) {
			expected.add( "xml" );
		}
		if ( discovered ) {
			expected.add( "discovered" );
		}
		if ( !excludeDefaults ) {
			if ( namedModule ) {
				expected.add( "module" );
			}
			expected.add( "package" );
		}
		if ( !excludeSuperclass ) {
			expected.add( "superclass-listener" );
		}
		expected.add( "explicit-listener" );
		expected.add( "entity-callback" );
		assertThat( events ).containsExactlyElementsOf( expected );
	}
}
