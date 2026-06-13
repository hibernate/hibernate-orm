/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.staticquery;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.query.NativeQuery;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

interface InvalidNativeStaticQuery {
	@NativeQuery("select * from Book")
	List<Book> nat(EntityAgent agent, Restriction<Book> restriction);
}
