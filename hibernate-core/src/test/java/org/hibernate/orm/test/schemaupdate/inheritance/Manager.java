/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance;

import java.io.Serializable;

/**
 * @author Andrea Boriero
 */
public class Manager extends Person implements Serializable {
}
