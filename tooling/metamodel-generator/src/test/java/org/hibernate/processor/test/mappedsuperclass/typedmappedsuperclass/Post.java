/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.typedmappedsuperclass;

import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Post<UserRoleType extends UserRole> {
}
