/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Parses join expressions for CockroachDBDialect.
 * <p>
 * We need to (re)parse the join expression in order to add join hints
 * to the generated SQL statement.
 *
 * @author Karel Maesen
 */
public class CockroachDialectParseJoinExpression {

	final private Pattern JOIN_PATTERN = Pattern.compile(
			"(?i)\\b(cross|natural\\s+(.*)\\b|(full|left|right)(\\s+outer)?)?\\s+join" );

	@Test
	public void testSimpleJoin() {
		Matcher m = JOIN_PATTERN.matcher( "abc join def" );
		if ( m.find() ) {
			assertNull( m.group( 1 ) );
		}
		else {
			Assertions.fail( "No match" );
		}
	}

	@Test
	public void testCross() {
		testJoinMatch( "CRoss join", 1, "cross" );
	}

	@Test
	public void testNatural() {
		testJoinMatch( "natural left outer join", 1, "natural left outer" );
	}

	@Test
	public void testLeftOuterJoin() {
		testJoinMatch( "left outer join", 1, "left outer" );
	}

	@Test
	public void testLeftJoin() {
		testJoinMatch( "left join", 1, "left" );
	}

	@Test
	public void testNaturalJoinType() {
		testJoinMatch( "natural left outer join", 2, "left outer" );
	}

	public void testJoinMatch(String input, int group, String expects) {
		testMatch( JOIN_PATTERN, input, group, expects );
	}

	public void testMatch(Pattern pattern, String input, int group, String expects) {
		Matcher m = pattern.matcher( input );
		if ( m.find() ) {
			Assertions.assertEquals( expects, m.group( group ).toLowerCase() );
		}
		else {
			Assertions.fail( "No match" );
		}
	}


}
