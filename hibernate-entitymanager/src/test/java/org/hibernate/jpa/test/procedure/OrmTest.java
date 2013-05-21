package org.hibernate.jpa.test.procedure;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class OrmTest extends AbstractStoredProcedureTest{
	@Override
	public String[] getEjb3DD() {
		return new String[]{"org/hibernate/jpa/test/procedure/orm.xml"};
	}
}
