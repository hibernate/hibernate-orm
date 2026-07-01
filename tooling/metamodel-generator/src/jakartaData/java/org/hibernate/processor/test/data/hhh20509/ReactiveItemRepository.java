/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20509;

import io.smallrye.mutiny.Uni;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;

@Repository
public interface ReactiveItemRepository {

	Mutiny.StatelessSession session();

	@Save
	Uni<Void> save(Item item);

	@Save
	Uni<List<Item>> saveAll(List<Item> items);
}
