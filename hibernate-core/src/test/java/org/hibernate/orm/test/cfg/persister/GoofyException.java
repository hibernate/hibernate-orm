/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.persister;

/**
 * @author Emmanuel Bernard
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
