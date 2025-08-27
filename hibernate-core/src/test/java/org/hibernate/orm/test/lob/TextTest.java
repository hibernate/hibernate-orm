/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Test eager materialization and mutation data mapped by
 * #{@link org.hibernate.type.StandardBasicTypes#TEXT}.
 *
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/lob/TextMappings.hbm.xml"
)
public class TextTest extends LongStringTest {
}
