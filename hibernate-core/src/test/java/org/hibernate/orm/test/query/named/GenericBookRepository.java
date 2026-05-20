/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;

import jakarta.persistence.query.JakartaQuery;

public interface GenericBookRepository<T> {
	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	List<T> inheritedGenericFindByTitle(String title);
}
