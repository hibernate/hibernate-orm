/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cut.generic;

public class EnumPlaceholder<T extends Enum<T>, R extends Enum<R>> {
	private final T firstEnum;
	private final R secondEnum;

	public EnumPlaceholder(T firstEnum, R secondEnum) {
		this.firstEnum = firstEnum;
		this.secondEnum = secondEnum;
	}

	public T getFirstEnum() {
		return firstEnum;
	}

	public R getSecondEnum() {
		return secondEnum;
	}
}
