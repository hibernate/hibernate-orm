/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh17613;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class ChildB<A> extends Parent {
}
