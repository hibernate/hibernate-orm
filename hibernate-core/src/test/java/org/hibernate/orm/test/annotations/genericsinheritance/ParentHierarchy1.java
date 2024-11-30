/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.Entity;

@Entity
public class ParentHierarchy1 extends Parent<ChildHierarchy1> {

}
