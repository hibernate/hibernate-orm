/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;

import jakarta.persistence.query.JakartaQuery;

public interface BookRepositoryQueries {
	@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
	List<Jpa4StaticQueryRegistrationTest.Book> inheritedFindByTitle(String title);
}
