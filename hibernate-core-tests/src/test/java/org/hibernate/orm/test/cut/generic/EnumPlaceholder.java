package org.hibernate.orm.test.cut.generic;

import java.io.Serializable;

public class EnumPlaceholder<T extends Enum<T>, R extends Enum<R>> implements Serializable {
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
