package org.hibernate.dialect;

import java.util.Map;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of the behavior of the H2Dialect
 *
 * @author Mayur Bhindi
 */
public class H2DialectTest {

	private H2Dialect dialect;

	@Before
	public void setup() {
		dialect = new H2Dialect();
	}

	@After
	public void tearDown() {
		dialect = null;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-15228")
	public void testRoundFunction() {
		Map<String, SQLFunction> functions = dialect.getFunctions();
		SQLFunction sqlFunction = functions.get( "round" );
		assertEquals( StandardBasicTypes.DOUBLE, sqlFunction.getReturnType( null, null ) );
		assertEquals( StandardBasicTypes.DOUBLE, sqlFunction.getReturnType( StandardBasicTypes.INTEGER, null ) );
		assertEquals(
				StandardBasicTypes.BIG_DECIMAL,
				sqlFunction.getReturnType( StandardBasicTypes.BIG_DECIMAL, null )
		);

	}
}
