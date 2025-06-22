/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated.ormXml;

/**
 * @author Oliverio
 * @author Steve Ebersole
 */
public class BookWithOrmEnum {
	private Long id;
	private Binding bindingOrdinalEnum;
	private Binding bindingStringEnum;

	public Long getId() {
		return id;
	}

	public Binding getBindingOrdinalEnum() {
		return bindingOrdinalEnum;
	}

	public void setBindingOrdinalEnum(Binding bindingOrdinalEnum) {
		this.bindingOrdinalEnum = bindingOrdinalEnum;
	}

	public Binding getBindingStringEnum() {
		return bindingStringEnum;
	}

	public void setBindingStringEnum(Binding bindingStringEnum) {
		this.bindingStringEnum = bindingStringEnum;
	}
}
