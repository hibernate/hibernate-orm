package org.hibernate.test.annotations.index.jpa;


import org.hibernate.testing.FailureExpectedWithNewMetamodel;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@FailureExpectedWithNewMetamodel
public class OrmXmlIndexTest extends AbstractJPAIndexTest {
	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/test/annotations/index/jpa/orm-index.xml" };
	}
}
