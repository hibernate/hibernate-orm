/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20509;

import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.StatelessSession;

import java.util.List;

@Repository
public interface ItemRepository {

	StatelessSession session();

	@Save
	void save(Item item);

	@Save
	List<Item> saveAll(List<Item> items);
}
