/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.inheritance;

import java.io.Serializable;

/**
 * @author Andrea Boriero
 */
public class Employee extends Person implements Serializable {
}
