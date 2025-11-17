/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.superdao.generic;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Save;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public interface SuperRepo<T,K> {

	@Save
	<S extends T> S save(S entity);

	@Save
	<S extends T> List<S> saveAll(List<S> entities);

	@Find
	Optional<T> findById(@By("id(this)") K id);

	@Find
	Optional<T> findById2(@By("id(this)") K id);

	@Find
	Stream<T> findAll();

	@Find
	Page<T> findAll(PageRequest pageRequest, Order<T> order);

//	@Delete
//	void deleteById(@By("id(this)") K id);

	@Delete
	void delete(T entity);

	@Delete
	void deleteAll(List<? extends T> entities);
}
