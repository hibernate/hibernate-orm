/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named;

import java.util.List;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository( provider = "query-binder-test" )
public interface CompanionRepository$ {
	@Query( "from Jpa4StaticQueryBook where title = :title" )
	List<Book> findByTitle(String title);
}
