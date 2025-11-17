/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.bytecode;

/**
 * @author Emmanuel Bernard
 */
public interface Tool {
	Long getId();
	void setId(Long id);

	Number usage();
}
