/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "HHH-13604")
public class CheckClassLookupTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Test(expected = CheckForbiddenAPIException.class)
	public void classLookupAtRuntime() throws Exception {
		Class.forName( "org.hibernate.session.runtime.check.Book" );
	}

	@Test
	@SuppressWarnings("unused")
	public void classLoadedAtRuntime_classInitialization() {
		Class useClass = ClassLoadedWithinClassInitialization.CLASS;
	}

	@Test(expected = CheckForbiddenAPIException.class)
	public void classLoadedAtRuntime_instanceInitialization() throws Exception {
		new ClassLoadedWithinInstanceInitialization();
	}

	public static class ClassLoadedWithinClassInitialization {
		private static final Class<?> CLASS;

		static {
			Class<?> readClass = null;
			try {
				readClass = Class.forName( "org.hibernate.session.runtime.check.Book" );
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException( e );
			}
			finally {
				CLASS = readClass;
			}
		}
	}

	public static class ClassLoadedWithinInstanceInitialization {
		public ClassLoadedWithinInstanceInitialization() throws ClassNotFoundException {
			Class.forName( "org.hibernate.session.runtime.check.Book" );
		}
	}
}
