package org.hibernate.orm.test.jpa.procedure;


import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Strong Liu
 */
@Jpa(annotatedClasses = {User.class})
public class AnnotationTest extends AbstractStoredProcedureTest {
}
