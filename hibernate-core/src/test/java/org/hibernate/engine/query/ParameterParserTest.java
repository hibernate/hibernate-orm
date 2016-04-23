/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query;

import org.junit.Test;

import org.hibernate.engine.query.spi.ParameterParser;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests of the ParameterParser class
 *
 * @author Steve Ebersole
 */
public class ParameterParserTest extends BaseUnitTestCase {
	@Test
	public void testEscapeCallRecognition() {
		assertTrue( ParameterParser.startsWithEscapeCallTemplate( "{ ? = call abc(?) }" ) );
		assertFalse( ParameterParser.startsWithEscapeCallTemplate(
				"from User u where u.userName = ? and u.userType = 'call'"
		) );
	}
}
