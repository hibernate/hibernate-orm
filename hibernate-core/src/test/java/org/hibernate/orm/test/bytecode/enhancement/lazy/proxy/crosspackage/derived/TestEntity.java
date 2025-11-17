/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.derived;

import jakarta.persistence.Entity;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base.BaseEntity;

@Entity
public class TestEntity extends BaseEntity {

}
