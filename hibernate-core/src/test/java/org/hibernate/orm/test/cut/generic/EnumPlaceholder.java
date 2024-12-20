/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
