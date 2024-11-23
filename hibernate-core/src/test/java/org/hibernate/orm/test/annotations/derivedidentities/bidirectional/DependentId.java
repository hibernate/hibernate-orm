/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;


/**
 * @author Hardy Ferentschik
 */
public class DependentId {
	long emp; // matches name of @Id attribute and type of Employee PK
}
