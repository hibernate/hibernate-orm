package org.hibernate.processor.test.util;

import javax.tools.Diagnostic;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The diagnostics reported by the compiler for the classes of a {@link CompilationTest}.
 * A test method may declare a parameter of this type, and should be annotated
 * {@link IgnoreCompilationErrors} if it expects errors.
 */
public final class CompilationDiagnostics {
	private final List<Diagnostic<?>> diagnostics;

	CompilationDiagnostics(List<Diagnostic<?>> diagnostics) {
		this.diagnostics = List.copyOf( diagnostics );
	}

	/**
	 * The messages of the compilation errors.
	 */
	public List<String> errorMessages() {
		return diagnostics.stream()
				.filter( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
				.map( diagnostic -> diagnostic.getMessage( Locale.ROOT ) )
				.toList();
	}

	/**
	 * Asserts that there is a compilation error whose message contains the given text.
	 */
	public void assertError(String text) {
		assertTrue( countErrors( text ) > 0,
				() -> "No compilation error containing '" + text + "' in:\n" + String.join( "\n", errorMessages() ) );
	}

	/**
	 * Asserts that there is exactly one compilation error whose message contains the given text.
	 */
	public void assertSingleError(String text) {
		assertEquals( 1, countErrors( text ),
				() -> "Compilation errors containing '" + text + "' in:\n" + String.join( "\n", errorMessages() ) );
	}

	private long countErrors(String text) {
		return errorMessages().stream()
				.filter( message -> message.contains( text ) )
				.count();
	}
}
