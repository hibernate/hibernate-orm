/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

import java.util.regex.Pattern;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "HHH-13604")
public class CheckPatternCompileTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Test( expected = CheckForbiddenAPIException.class )
	public void useRegexAtRuntime() {
		Pattern.compile( "aaa" );
	}

	@Test
	@SuppressWarnings("unused")
	public void useRegexAtRuntime_classInitialization() {
		Pattern useClass = PatterCompileWithinClassInitialization.PATTERN;
	}

	@Test( expected = CheckForbiddenAPIException.class )
	public void useRegexAtRuntime_instanceInitialization() {
		new PatterCompileWithinInstanceInitialization();
	}

	public static class PatterCompileWithinClassInitialization {
		private static final Pattern PATTERN = Pattern.compile( "aaa" );
	}

	public static class PatterCompileWithinInstanceInitialization {
		public PatterCompileWithinInstanceInitialization() {
			Pattern.compile( "aaa" );
		}
	}
}
