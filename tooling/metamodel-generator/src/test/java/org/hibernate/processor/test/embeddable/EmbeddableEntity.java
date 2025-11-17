/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddable;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;

/* Here the getter is mandatory to reproduce the issue. No @Access(FIELD) annotation. */
@Embeddable
//@Access(AccessType.FIELD)
public class EmbeddableEntity {
	@OneToMany(targetEntity = Stuff.class)
	Set<IStuff> stuffs = new HashSet<IStuff>();

	public Set<IStuff> getStuffs() {
		return stuffs;
	}
}
