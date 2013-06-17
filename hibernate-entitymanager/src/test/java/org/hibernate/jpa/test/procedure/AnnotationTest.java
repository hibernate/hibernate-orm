package org.hibernate.jpa.test.procedure;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@FailureExpectedWithNewMetamodel
public class AnnotationTest extends AbstractStoredProcedureTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}
}
