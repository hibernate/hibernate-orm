/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.index_out_of_bounds.derived;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.index_out_of_bounds.base.BaseEntity;

@Entity
@Access(AccessType.PROPERTY)
public class TestEntity extends BaseEntity {

}
