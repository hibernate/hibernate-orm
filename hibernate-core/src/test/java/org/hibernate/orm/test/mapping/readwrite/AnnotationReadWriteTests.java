/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.readwrite;

import org.hibernate.testing.orm.junit.DomainModel;

@DomainModel( annotatedClasses = ReadWriteEntity.class )
public class AnnotationReadWriteTests extends AbstractReadWriteTests {
}
