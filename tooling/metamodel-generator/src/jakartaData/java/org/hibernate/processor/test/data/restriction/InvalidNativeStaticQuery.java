/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import jakarta.data.restrict.Restriction;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.query.JakartaQuery;
import jakarta.persistence.query.NativeQuery;

import java.util.List;

interface InvalidNativeStaticQuery {
	record Summary(String isbn, String title) {
	}

	@JakartaQuery("from DataRestrictionBook")
	List<DataRestrictionBook> books();

	@JakartaQuery("from DataRestrictionBook")
	List<Summary> summaries();

	@NativeQuery("select * from data_restriction_book")
	List<DataRestrictionBook> nat(EntityAgent agent, Restriction<DataRestrictionBook> restriction);
}
