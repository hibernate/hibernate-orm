/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.procedure;

import org.hibernate.testing.orm.junit.Jpa;

/**
 * @author Strong Liu
 */
@Jpa(xmlMappings = {"org/hibernate/jpa/test/procedure/orm.xml"})
public class OrmTest extends AbstractStoredProcedureTest{
}
