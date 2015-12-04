package org.hibernate.test.tool.schema;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.tool.schema.internal.TargetStdoutImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Koen Aers
 */
public class TargetStdoutImplTest {
	
	private static final Formatter FORMATTER = FormatStyle.DDL.getFormatter();
	private static final String BEFORE_FORMAT = 
			"create table test_entity (field varchar(255) not null, primary key (field))";
	private static final String AFTER_FORMAT = FORMATTER.format(BEFORE_FORMAT);
	private static final String DELIMITER = "---";
	
	private PrintStream savedSystemOut = null;
	private ByteArrayOutputStream baos = null;
	private TargetStdoutImpl target = null;
	
	@Before
	public void setUp() {
		savedSystemOut = System.out;
		baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
	}
	
	@After
	public void tearDown() {
		System.setOut(savedSystemOut);
	}
	
	@Test
	public void testAcceptWithFormatter() {
		target = new TargetStdoutImpl(DELIMITER, FORMATTER);
		target.accept(BEFORE_FORMAT);
		Assert.assertEquals(
				AFTER_FORMAT + DELIMITER + System.lineSeparator(), 
				baos.toString());
	}
	
}
