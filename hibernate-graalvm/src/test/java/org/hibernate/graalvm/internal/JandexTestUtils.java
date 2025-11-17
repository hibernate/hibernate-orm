/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graalvm.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;

public final class JandexTestUtils {

	private JandexTestUtils() {
	}

	public static Index indexJar(Class<?> clazz) {
		return indexClasses( determineJarLocation( clazz ) );
	}

	private static URI determineJarLocation(Class<?> clazz) {
		URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		try {
			return url.toURI();
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException( "Cannot retrieve URI for JAR of " + clazz + "?", e );
		}
	}

	private static Index indexClasses(URI classesUri) {
		try ( CodeSource cs = CodeSource.open( classesUri ) ) {
			Indexer indexer = new Indexer();
			try ( Stream<Path> stream = Files.walk( cs.getRoot() ) ) {
				for ( Iterator<Path> it = stream.iterator(); it.hasNext(); ) {
					Path path = it.next();
					if ( path.getFileName() == null || !path.getFileName().toString().endsWith( ".class" ) ) {
						continue;
					}
					try ( InputStream inputStream = Files.newInputStream( path ) ) {
						indexer.index( inputStream );
					}
				}
			}
			return indexer.complete();
		}
		catch (RuntimeException | IOException e) {
			throw new IllegalStateException( "Cannot index classes at " + classesUri, e );
		}
	}

	public static Class<?> load(DotName className) {
		try {
			return Class.forName( className.toString() );
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException( "Could not load class " + className, e );
		}
	}

	public static Set<Class<?>> findConcreteNamedImplementors(Index index, Class<?>... interfaces) {
		return Arrays.stream( interfaces ).map( DotName::createSimple )
				.flatMap( n -> findConcreteNamedImplementors( index, n ).stream() )
				.collect( Collectors.toSet() );
	}

	private static Set<Class<?>> findConcreteNamedImplementors(Index index, DotName interfaceDotName) {
		assertThat( index.getClassByName( interfaceDotName ) ).isNotNull();
		return index.getAllKnownImplementors( interfaceDotName ).stream()
				.filter( c -> !c.isInterface()
						// Ignore anonymous classes
						&& c.simpleName() != null )
				.map( ClassInfo::name )
				.map( JandexTestUtils::load )
				.filter( c -> ( c.getModifiers() & Modifier.ABSTRACT ) == 0 )
				.collect( Collectors.toSet() );
	}

}
