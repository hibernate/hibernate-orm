/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.Entity;

@Entity
public class ParentHierarchy22 extends ParentHierarchy2<ChildHierarchy22> {

}
