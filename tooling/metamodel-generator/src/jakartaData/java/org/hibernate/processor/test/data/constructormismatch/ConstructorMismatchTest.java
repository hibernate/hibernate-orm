package org.hibernate.processor.test.data.constructormismatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestUtil;
import org.junit.jupiter.api.Test;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class ConstructorMismatchTest {
	@Test
	void reportsSelectionAndConstructorTypes() throws Exception {
		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		assertFalse( compile( InvalidSummaryRepository.class, diagnostics, List.of() ) );
		final String message = diagnostics.getDiagnostics().stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.filter( diagnostic -> diagnostic.contains( "has no constructor matching query selection list" ) )
				.collect( Collectors.joining( "\n" ) );
		assertFalse( message.isEmpty(), diagnostics.getDiagnostics().toString() );
		// The generic dialect does not register array_agg, so its inferred type is Object.
		assertTrue( message.contains( "with types (java.lang.String, java.lang.Object)" ), message );
		assertTrue( message.contains( "available constructor parameter lists: [(java.lang.String, int)]" ), message );
	}

	@Test
	void reportsArraySelectionTypesWithDialect() throws Exception {
		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		assertFalse( compile( InvalidSummaryRepository.class, diagnostics,
				List.of( "-A" + HibernateProcessor.DIALECT_OPTION + "=org.hibernate.dialect.PostgreSQLDialect" ) ) );
		final String message = diagnostics.getDiagnostics().stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );
		assertTrue( message.contains( "with types (java.lang.String, java.lang.Integer[])" ), message );
		assertTrue( message.contains( "available constructor parameter lists: [(java.lang.String, int)]" ), message );
	}

	@Test
	void reportsConstructorTypesForWrongArity() throws Exception {
		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		assertFalse( compile( InvalidArityRepository.class, diagnostics, List.of() ) );
		final String message = diagnostics.getDiagnostics().stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.collect( Collectors.joining( "\n" ) );
		assertTrue( message.contains( "with types (java.lang.String, java.lang.Integer, java.lang.Long)" ), message );
		assertTrue( message.contains( "available constructor parameter lists:" ), message );
		assertTrue( message.contains( "(java.lang.String)" ), message );
		assertTrue( message.contains( "(java.util.List<java.lang.String>, int[])" ), message );
		final String constructors = message.substring( message.indexOf( "available constructor parameter lists: " )
				+ "available constructor parameter lists: ".length() );
		assertTrue( constructors.length() <= 256, message );
		assertTrue( constructors.endsWith( "...]" ), message );
	}

	@Test
	void boundsLargeSelectionTypeList() throws Exception {
		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		assertFalse( compile( InvalidLargeSelectionRepository.class, diagnostics, List.of() ) );
		final String message = diagnostics.getDiagnostics().stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.filter( diagnostic -> diagnostic.contains( "has no constructor matching query selection list" ) )
				.findFirst().orElseThrow();
		final String types = message.substring( message.indexOf( " with types " ) + " with types ".length(),
				message.indexOf( "; available constructor parameter lists:" ) );
		assertTrue( types.startsWith( "(java.lang.String, " ), message );
		assertTrue( types.length() <= 256, message );
		assertTrue( types.endsWith( "...)" ), message );
	}

	@Test
	void matchingConstructorStillCompiles() throws Exception {
		final var diagnostics = new DiagnosticCollector<JavaFileObject>();
		assertTrue( compile( ValidSummaryRepository.class, diagnostics, List.of() ), diagnostics.getDiagnostics().toString() );
	}

	private boolean compile(
			Class<?> repository,
			DiagnosticCollector<JavaFileObject> diagnostics,
			List<String> processorOptions) throws Exception {
		final var compiler = ToolProvider.getSystemJavaCompiler();
		try ( var fileManager = compiler.getStandardFileManager( diagnostics, Locale.ROOT, defaultCharset() ) ) {
			final var sourceFiles = List.of(
					TestUtil.getSourceFile( Book.class ),
					TestUtil.getSourceFile( Summary.class ),
					TestUtil.getSourceFile( OverloadedSummary.class ),
					TestUtil.getSourceFile( repository )
			);
			final var options = new ArrayList<>( List.of(
					"-d", TestUtil.getOutBaseDir( ConstructorMismatchTest.class ).getAbsolutePath(),
					"-processor", HibernateProcessor.class.getName()
			) );
			options.addAll( processorOptions );
			return compiler.getTask(
					null, fileManager, diagnostics,
					options,
					null, fileManager.getJavaFileObjectsFromFiles( sourceFiles )
			).call();
		}
	}
}
