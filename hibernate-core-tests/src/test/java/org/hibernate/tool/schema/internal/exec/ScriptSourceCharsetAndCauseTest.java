package org.hibernate.tool.schema.internal.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema script inputs must keep the IOException that prevented the script from
 * opening, and must close the stream when the charset name is rejected.
 */
@JiraKey("HHH-21196")
public class ScriptSourceCharsetAndCauseTest {

	private static final String BAD_CHARSET = "not-a-real-charset";

	@Test
	public void urlOpenFailureKeepsTheCause(@TempDir File tempDir) throws IOException {
		final File missing = File.createTempFile( "schema-script-missing", ".sql", tempDir );
		assertTrue( missing.delete() );
		final ScriptSourceInputFromUrl input = new ScriptSourceInputFromUrl( missing.toURI().toURL(), "UTF-8" );

		final SchemaManagementException exception = assertThrows(
				SchemaManagementException.class,
				() -> input.extract( reader -> List.of() )
		);

		assertInstanceOf( FileNotFoundException.class, exception.getCause() );
	}

	@Test
	public void badCharsetReleasesTheUrlStream(@TempDir File tempDir) throws IOException {
		final File script = scriptFile(tempDir);
		final ScriptSourceInputFromUrl input = new ScriptSourceInputFromUrl( script.toURI().toURL(), BAD_CHARSET );

		final SchemaManagementException exception = assertThrows(
				SchemaManagementException.class,
				() -> input.extract( reader -> List.of() )
		);

		assertInstanceOf( UnsupportedEncodingException.class, exception.getCause() );
		assertReleased( script );
	}

	@Test
	public void badCharsetReleasesTheFileStream(@TempDir File tempDir) throws IOException {
		final File script = scriptFile(tempDir);
		final ScriptSourceInputFromFile input = new ScriptSourceInputFromFile( script, BAD_CHARSET );

		final SchemaManagementException exception = assertThrows(
				SchemaManagementException.class,
				() -> input.extract( reader -> List.of() )
		);

		assertInstanceOf( UnsupportedEncodingException.class, exception.getCause() );
		assertReleased( script );
	}

	@Test
	public void badCharsetReleasesTheTargetStream(@TempDir File tempDir) throws IOException {
		final File script = scriptFile(tempDir);
		final ScriptTargetOutputToFile output = new ScriptTargetOutputToFile( script, BAD_CHARSET, false );

		final SchemaManagementException exception = assertThrows(
				SchemaManagementException.class,
				output::prepare
		);

		assertInstanceOf( UnsupportedEncodingException.class, exception.getCause() );
		assertReleased( script );
	}

	@Test
	public void utf8FileScriptIsRead(@TempDir File tempDir) throws IOException {
		final File script = scriptFile(tempDir);
		final ScriptSourceInputFromFile input = new ScriptSourceInputFromFile( script, "UTF-8" );

		final List<String> lines = input.extract( reader -> {
			try {
				return List.of( new BufferedReader( reader ).readLine() );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		} );

		assertEquals( "select 1;", lines.get( 0 ) );
		assertReleased( script );
	}

	private static File scriptFile(File tempDir) throws IOException {
		final File script = File.createTempFile( "schema-script", ".sql", tempDir );
		Files.writeString( script.toPath(), "select 1;\n" );
		return script;
	}

	private static void assertReleased(File script) {
		assertTrue( script.delete(), "schema script stayed open: " + script.getAbsolutePath() );
	}
}
