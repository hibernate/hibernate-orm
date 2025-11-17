/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity(name="GenericKey")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name="PP_GenericDCKey")
public abstract class GenericKey extends AbstractKey implements Serializable {

}
