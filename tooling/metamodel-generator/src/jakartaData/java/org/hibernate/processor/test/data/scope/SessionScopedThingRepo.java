/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.scope;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.enterprise.context.SessionScoped;

@SessionScoped
@Repository
public interface SessionScopedThingRepo extends CrudRepository<Thing, Thing.Id> {
	@Find
	Thing thing(Thing.Id id);
}
