package org.hibernate.test.annotations.index.jpa;


/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class OrmXmlIndexTest extends AbstractJPAIndexTest {
	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/index/jpa/orm-index.xml" };
	}
}
