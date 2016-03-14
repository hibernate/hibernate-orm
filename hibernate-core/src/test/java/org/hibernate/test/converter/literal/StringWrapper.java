/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter.literal;

/**
 * @author Janario Oliveira
 */
public class StringWrapper {
	private final String value;

	public StringWrapper(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
