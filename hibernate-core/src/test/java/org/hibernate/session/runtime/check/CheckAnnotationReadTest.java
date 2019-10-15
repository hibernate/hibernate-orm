/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

import javax.persistence.Entity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "HHH-13604")
public class CheckAnnotationReadTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class };
	}

	@Test(expected = CheckForbiddenAPIException.class)
	public void readAnnotationAtRuntime() {
		Book.class.getAnnotation( Entity.class );
	}

	@Test
	@SuppressWarnings("unused")
	public void readAnnotationAtRuntime_classInitialization() {
		Entity useClass = AnnotationReadWithinClassInitialization.ANNOTATION;
	}

	@Test(expected = CheckForbiddenAPIException.class)
	public void readAnnotationAtRuntime_instanceInitialization() {
		new AnnotationReadWithinInstanceInitialization();
	}

	public static class AnnotationReadWithinClassInitialization {
		private static final Entity ANNOTATION = Book.class.getAnnotation( Entity.class );
	}

	public static class AnnotationReadWithinInstanceInitialization {
		public AnnotationReadWithinInstanceInitialization() {
			Book.class.getAnnotation( Entity.class );
		}
	}
}
