/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance.tableperclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;


/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "CATEGORY")
public class Category extends Element {
}
