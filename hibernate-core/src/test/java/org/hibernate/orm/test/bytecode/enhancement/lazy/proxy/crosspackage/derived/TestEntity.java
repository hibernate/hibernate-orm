/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.derived;

import jakarta.persistence.Entity;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base.BaseEntity;

@Entity
public class TestEntity extends BaseEntity {

}
