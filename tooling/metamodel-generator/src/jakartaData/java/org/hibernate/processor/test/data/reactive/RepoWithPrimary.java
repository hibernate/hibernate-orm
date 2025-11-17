/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import org.hibernate.reactive.mutiny.Mutiny;

@Repository
public interface RepoWithPrimary {
	Uni<Mutiny.StatelessSession> session(); //required
	@Insert
	Uni<Void> insert(Book book);
	@Delete
	Uni<Void> delete(Book book);
	@Delete
	Uni<Void> deleteById(String isbn);
}
