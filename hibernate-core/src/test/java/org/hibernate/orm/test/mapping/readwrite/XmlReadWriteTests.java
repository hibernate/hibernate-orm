/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.readwrite;

import org.hibernate.testing.orm.junit.DomainModel;

@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/readwrite/ReadWriteEntity.orm.xml" )
public class XmlReadWriteTests extends AbstractReadWriteTests {
}
