/*
 * SPDX-License-Identifier: Apache-2.0
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
