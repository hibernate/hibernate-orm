/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;


/**
 * @author Strong Liu
 */
public class OrmXmlIndexTest extends AbstractJPAIndexTest {
	@Override
	protected String[] getXmlFiles() {
		return new String[] { "org/hibernate/orm/test/annotations/index/jpa/orm-index.xml" };
	}
}
