/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

/**
 * @author Strong Liu
 */
public class OrmTest extends AbstractStoredProcedureTest{
	@Override
	public String[] getEjb3DD() {
		return new String[]{"org/hibernate/jpa/test/procedure/orm.xml"};
	}
}
