/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity(name="MoreSpecializedKey")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class MoreSpecializedKey extends SpecializedKey implements Serializable {
}
