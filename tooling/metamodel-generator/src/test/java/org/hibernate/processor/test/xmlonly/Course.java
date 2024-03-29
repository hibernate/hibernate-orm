/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.xmlonly;

import java.util.Set;

public class Course {
	private Long id;
	private String name;
	private Set<Teacher> qualifiedTeachers;
}
