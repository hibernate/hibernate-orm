/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.stateful;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static java.nio.charset.Charset.defaultCharset;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class StatefulRepositoryTest {
	@Test
	@WithClasses({ StatefulBook.class, StatefulBookRepository.class })
	void statefulRepositoryUsesStatefulSessionAndLifecycleOperations() {
		final String repository = getMetaModelSourceAsString( StatefulBookRepository.class, true );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( StatefulBook.class );
		assertMetamodelClassGeneratedFor( StatefulBookRepository.class, true );

		assertTrue( repository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( repository.contains( "public @Nonnull EntityManager entityManager()" ) );
		assertFalse( repository.contains( "openStatelessSession()" ) );
		assertTrue( repository.contains( "@EntityListener" ) );
		assertTrue( repository.contains( "private Event<LifecycleEvent<?>> event;" ) );

		assertTrue( repository.contains( "entityManager.createQuery(_query)" ) );
		assertTrue( repository.contains( "entityManager.persist(book);" ) );
		assertTrue( repository.contains( "for (var _entity : books)" ) );
		assertTrue( repository.contains( "entityManager.persist(_entity);" ) );
		assertTrue( repository.contains( "book = entityManager.merge(book);" ) );
		assertTrue( repository.contains( "List<StatefulBook> _result;" ) );
		assertTrue( repository.contains( "_result = new ArrayList<>();" ) );
		assertTrue( repository.contains( "_result.add(entityManager.merge(_entity));" ) );
		assertTrue( repository.contains( "_result = books.clone();" ) );
		assertTrue( repository.contains( "_result[_index] = entityManager.merge(books[_index]);" ) );
		assertTrue( repository.contains( "entityManager.refresh(book);" ) );
		assertTrue( repository.contains( "entityManager.remove(book);" ) );
		assertTrue( repository.contains( "entityManager.detach(book);" ) );

		assertTrue( repository.contains( "@PreInsert" ) );
		assertTrue( repository.contains( "void _onPreInsert(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PreInsertEvent<>(entity));" ) );
		assertTrue( repository.contains( "@PostInsert" ) );
		assertTrue( repository.contains( "void _onPostInsert(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PostInsertEvent<>(entity));" ) );
		assertTrue( repository.contains( "@PreUpdate" ) );
		assertTrue( repository.contains( "void _onPreUpdate(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PreUpdateEvent<>(entity));" ) );
		assertTrue( repository.contains( "@PostUpdate" ) );
		assertTrue( repository.contains( "void _onPostUpdate(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PostUpdateEvent<>(entity));" ) );
		assertTrue( repository.contains( "@PreDelete" ) );
		assertTrue( repository.contains( "void _onPreDelete(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PreDeleteEvent<>(entity));" ) );
		assertTrue( repository.contains( "@PostDelete" ) );
		assertTrue( repository.contains( "void _onPostDelete(StatefulBook entity)" ) );
		assertTrue( repository.contains( ".fire(new PostDeleteEvent<>(entity));" ) );
		assertFalse( repository.contains( "_hibernate_data_" ) );
	}

	@Test
	@WithClasses({ StatefulBook.class, EntityManagerStatefulBookRepository.class })
	void statefulRepositoryMayUseEntityManagerAccessor() {
		final String repository = getMetaModelSourceAsString( EntityManagerStatefulBookRepository.class, true );
		assertMetamodelClassGeneratedFor( EntityManagerStatefulBookRepository.class, true );

		assertTrue( repository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( repository.contains( "public @Nonnull EntityManager entityManager()" ) );
		assertFalse( repository.contains( "StatelessSession" ) );
		assertFalse( repository.contains( "createSelectionQuery(_query)" ) );
		assertTrue( repository.contains( "@EntityListener" ) );

		assertTrue( repository.contains( "entityManager.createQuery(_query)" ) );
		assertTrue( repository.contains( "entityManager.persist(book);" ) );
		assertTrue( repository.contains( "book = entityManager.merge(book);" ) );
		assertTrue( repository.contains( "entityManager.refresh(book);" ) );
		assertTrue( repository.contains( "entityManager.remove(book);" ) );
		assertTrue( repository.contains( "entityManager.detach(book);" ) );
	}

	@Test
	void defaultConstructorPathInjectsEntityManagerAndEntityAgent() throws IOException {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var fileManager = compiler.getStandardFileManager( diagnostics, Locale.ROOT, defaultCharset() ) ) {
			final var sourceFiles = List.of(
					sourceFile( StatefulBook.class ),
					sourceFile( StatefulBookRepository.class ),
					sourceFile( StatelessBookRepository.class )
			);
			final var options = List.of(
					"-d",
					TestUtil.getOutBaseDir( StatefulRepositoryTest.class ).getAbsolutePath(),
					"-classpath",
					classPathWithoutQuarkus(),
					"-processor",
					HibernateProcessor.class.getName()
			);
			final var task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					options,
					null,
					fileManager.getJavaFileObjectsFromFiles( sourceFiles )
			);
			assertTrue( task.call(), errorMessages( diagnostics ) );
		}

		final String statefulRepository = getMetaModelSourceAsString( StatefulBookRepository.class, true );
		assertTrue( statefulRepository.contains( "@PersistenceContext" ) );
		assertTrue( statefulRepository.contains( "protected @Nonnull EntityManager entityManager;" ) );
		assertTrue( statefulRepository.contains( "@Inject" + System.lineSeparator() + "\t_StatefulBookRepository()" ) );
		assertFalse( statefulRepository.contains( "EntityManagerFactory" ) );
		assertFalse( statefulRepository.contains( "createEntityManager()" ) );
		assertFalse( statefulRepository.contains( "entityManager.close();" ) );
		assertFalse( statefulRepository.contains( "@PostConstruct" ) );
		assertFalse( statefulRepository.contains( "@PreDestroy" ) );
		assertFalse( statefulRepository.contains( ".openStatelessSession();" ) );
		assertTrue( statefulRepository.contains( "@EntityListener" ) );
		assertTrue( statefulRepository.contains( ".fire(new PreInsertEvent<>(entity));" ) );
		assertTrue( statefulRepository.contains( ".fire(new PostDeleteEvent<>(entity));" ) );

		final String statelessRepository = getMetaModelSourceAsString( StatelessBookRepository.class, true );
		assertTrue( statelessRepository.contains( "@PersistenceAgent" ) );
		assertTrue( statelessRepository.contains( "protected @Nonnull EntityAgent entityAgent;" ) );
		assertTrue( statelessRepository.contains( "@Inject" + System.lineSeparator() + "\t_StatelessBookRepository()" ) );
		assertFalse( statelessRepository.contains( "EntityManagerFactory" ) );
		assertFalse( statelessRepository.contains( "createEntityAgent()" ) );
		assertFalse( statelessRepository.contains( "entityAgent.close();" ) );
		assertFalse( statelessRepository.contains( "@PostConstruct" ) );
		assertFalse( statelessRepository.contains( "@PreDestroy" ) );
		assertFalse( statelessRepository.contains( "SessionFactory.class).openSession();" ) );
		assertFalse( statelessRepository.contains( "@EntityListener" ) );
	}

	@Test
	void invalidStatefulRepositoryUsesMeaningfulDiagnostics() throws Exception {
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try ( StandardJavaFileManager fileManager = compiler.getStandardFileManager( diagnostics, null, null ) ) {
			final List<File> sourceFiles = List.of(
					sourceFile( StatefulBook.class ),
					sourceFile( InvalidStatefulBookRepository.class ),
					sourceFile( InvalidStatelessBackedStatefulBookRepository.class )
			);
			final JavaCompiler.CompilationTask task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					List.of(
							"-d",
							TestUtil.getOutBaseDir( StatefulRepositoryTest.class ).getAbsolutePath(),
							"-processor",
							HibernateProcessor.class.getName()
					),
					null,
					fileManager.getJavaFileObjectsFromFiles( sourceFiles )
			);
			assertFalse( task.call() );
		}
		final String messages = diagnostics.getDiagnostics()
				.stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );

		assertTrue( messages.contains( "repository mixes stateful and stateless lifecycle annotations" ) );
		assertTrue( messages.contains(
				"repository with stateful lifecycle methods must be backed by a 'Session' or 'EntityManager'" ) );
		assertTrue( messages.contains( "method annotated '@Persist' must be declared 'void'" ) );
		assertTrue( messages.contains( "method annotated '@Merge' must have the same return type as its parameter" ) );
	}

	private static String classPathWithoutQuarkus() {
		return Arrays.stream( System.getProperty( "java.class.path" ).split( Pattern.quote( File.pathSeparator ) ) )
				.filter( path -> !path.contains( "quarkus" ) )
				.collect( Collectors.joining( File.pathSeparator ) );
	}

	private static String errorMessages(DiagnosticCollector<JavaFileObject> diagnostics) {
		return diagnostics.getDiagnostics()
				.stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );
	}

	private static File sourceFile(Class<?> type) {
		return new File(
				TestUtil.getSourceBaseDir( type ),
				type.getName().replace( '.', File.separatorChar ) + ".java"
		);
	}
}
