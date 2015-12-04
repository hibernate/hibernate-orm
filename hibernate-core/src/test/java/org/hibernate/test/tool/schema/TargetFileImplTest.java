package org.hibernate.test.tool.schema;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.TargetFileImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Koen Aers
 */
public class TargetFileImplTest {

	private static final Formatter FORMATTER = FormatStyle.DDL.getFormatter();
	private static final String BEFORE_FORMAT = 
			"create table test_entity (field varchar(255) not null, primary key (field))";
	private static final String AFTER_FORMAT = FORMATTER.format(BEFORE_FORMAT);
	private static final String DELIMITER = "---";
	
	private File file = null;
	private String path = null;
	private TargetFileImpl target = null;
	
	@Before
	public void setUp() throws Exception {
		file = File.createTempFile("temp", "sql");
		file.deleteOnExit();
		path = file.getAbsolutePath();
	}

	@Test
	public void testAcceptWithFormatter() throws Exception {
		target = new TargetFileImpl(path, DELIMITER, FORMATTER);
		target.prepare();
		target.accept(BEFORE_FORMAT);
		target.release();
		Assert.assertEquals(
				AFTER_FORMAT + DELIMITER + System.lineSeparator(), 
				new String(Files.readAllBytes(file.toPath())));
	}
	
}
