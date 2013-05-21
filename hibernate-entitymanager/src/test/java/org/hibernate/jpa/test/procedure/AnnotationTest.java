package org.hibernate.jpa.test.procedure;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class AnnotationTest extends AbstractStoredProcedureTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}
}
