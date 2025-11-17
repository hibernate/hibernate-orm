/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * Tests eager materialization and mutation of data mapped by
 * {@link org.hibernate.type.StandardBasicTypes#IMAGE}.
 *
 * @author Gail Badner
 */
@DomainModel(xmlMappings = "org/hibernate/orm/test/lob/ImageMappings.hbm.xml")
public class ImageTest extends LongByteArrayTest {
}
