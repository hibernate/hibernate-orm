/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.defaultsemantics;

import java.net.URLConnection;
import java.nio.file.Path;

import jakarta.persistence.Entity;
import org.hibernate.annotations.DefaultListSemantics;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Bag;
import org.hibernate.orm.test.mapping.collections.defaultsemantics.modulefixtures.bag.BagOwner;
import org.hibernate.orm.test.mapping.collections.defaultsemantics.modulefixtures.list.ListOwner;
import org.hibernate.orm.test.mapping.collections.defaultsemantics.modulefixtures.plain.PlainOwner;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.module.TestModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Exercises real module annotations, including modules outside the boot layer.
/// @author Steve Ebersole
@JiraKey("HHH-20893")
class ModuleDefaultListSemanticsTests {
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

	@ParameterizedTest
	@ValueSource(strings = { "BAG", "LIST" })
	void moduleDefaultAndPackageOverride(String semantics) throws Exception {
		final var archive = ShrinkWrap.create( JavaArchive.class, "list-semantics.jar" )
				.addClass( PlainOwner.class )
				.addPackage( BagOwner.class.getPackage() )
				.addPackage( ListOwner.class.getPackage() );
		try (var module = TestModule.load( archive, """
				/// @author Steve Ebersole
				@org.hibernate.annotations.DefaultListSemantics(
					org.hibernate.annotations.DefaultListSemantics.Classification.%s)
				open module test.listsemantics {
				}
				""".formatted( semantics ), temporaryDirectory, getClass().getClassLoader(),
				DefaultListSemantics.class, Entity.class )) {
			final var plain = module.loadClass( PlainOwner.class.getName() );
			final var bag = module.loadClass( BagOwner.class.getName() );
			final var list = module.loadClass( ListOwner.class.getName() );
			final var bootstrap = new BootstrapServiceRegistryBuilder().applyClassLoader( module.classLoader() ).build();
			try (var registry = new StandardServiceRegistryBuilder( bootstrap )
					.applySetting( "hibernate.mapping.default_list_semantics", semantics.equals( "LIST" ) ? "BAG" : "LIST" )
					.build()) {
				final var metadata = new MetadataSources( registry )
						.addAnnotatedClass( plain ).addAnnotatedClass( bag ).addAnnotatedClass( list ).buildMetadata();
				assertThat( metadata.getEntityBinding( plain.getName() ).getProperty( "names" ).getValue() )
						.isInstanceOf( semantics.equals( "LIST" ) ? org.hibernate.mapping.List.class : Bag.class );
				assertThat( metadata.getEntityBinding( bag.getName() ).getProperty( "names" ).getValue() )
						.isInstanceOf( Bag.class );
				assertThat( metadata.getEntityBinding( list.getName() ).getProperty( "names" ).getValue() )
						.isInstanceOf( org.hibernate.mapping.List.class );
			}
			finally {
				BootstrapServiceRegistryBuilder.destroy( bootstrap );
			}
		}
	}
}
