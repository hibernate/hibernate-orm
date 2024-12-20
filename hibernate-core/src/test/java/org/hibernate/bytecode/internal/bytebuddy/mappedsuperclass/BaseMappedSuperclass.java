/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy.mappedsuperclass;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class BaseMappedSuperclass {

@Id
protected Long id;

protected long value;
}
