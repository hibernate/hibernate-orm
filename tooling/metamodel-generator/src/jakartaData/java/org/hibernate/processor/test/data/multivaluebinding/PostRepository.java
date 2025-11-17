/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.multivaluebinding;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends DataRepository<Post, Integer> {

	@Query("from Post p where p.name in (:names)")
	List<Post> getPostsByName(Collection<String> names);
}
