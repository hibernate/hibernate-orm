/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;

/**
 * @author Steve Ebersole
 */
@Entity
@NamedEntityGraph
public class Person {
	@Id
	public Long id;
}
