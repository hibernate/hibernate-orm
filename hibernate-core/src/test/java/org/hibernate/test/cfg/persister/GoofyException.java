/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
public class GoofyException extends RuntimeException {
	private Class<?> value;

	public GoofyException(Class<?> value) {
		this.value = value;
	}

	public Class<?> getValue() {
		return value;
	}
}
