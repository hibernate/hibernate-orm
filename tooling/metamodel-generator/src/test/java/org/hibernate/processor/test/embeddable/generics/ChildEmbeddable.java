/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable.generics;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class ChildEmbeddable extends ParentEmbeddable<MyTypeImpl> {
}
