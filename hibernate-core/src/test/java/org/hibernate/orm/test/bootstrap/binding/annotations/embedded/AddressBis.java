/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;

@Embeddable
public class AddressBis {

	@Formula("2")
	Integer formula;
}
