/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;


import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Strong Liu
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/annotations/index/jpa/orm-index.xml"
)
public class OrmXmlIndexTest extends AbstractJPAIndexTest {
}
