/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which writes the order column from
 * {@code AbstractOneToManyDecomposer}.
 *
 * @author Donghwan Kim
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseListNullElementTest.ListParent.class,
				AbstractInverseListNullElementTest.ListChild.class
		}
)
@SessionFactory
public class InverseListNullElementTest extends AbstractInverseListNullElementTest {
}
