package org.hibernate.jpa.test.procedure;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@FailureExpectedWithNewMetamodel
public class OrmTest extends AbstractStoredProcedureTest{
	@Override
	public String[] getEjb3DD() {
		return new String[]{"org/hibernate/jpa/test/procedure/orm.xml"};
	}
}
