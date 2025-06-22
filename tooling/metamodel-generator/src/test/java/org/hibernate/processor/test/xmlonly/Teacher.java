/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlonly;

import java.util.Set;

public class Teacher {
	private Long id;
	private Set<Course> qualifiedFor;
}
