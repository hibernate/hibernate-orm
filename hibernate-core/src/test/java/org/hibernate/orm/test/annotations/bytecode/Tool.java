/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.bytecode;


/**
 * @author Emmanuel Bernard
 */
public interface Tool {
	public Long getId();

	public void setId(Long id);

	public Number usage();
}
