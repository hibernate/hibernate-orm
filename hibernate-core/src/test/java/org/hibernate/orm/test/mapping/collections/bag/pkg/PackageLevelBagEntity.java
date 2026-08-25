/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.bag.pkg;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "PackageLevelBagEntity")
@Table(name = "pkg_bag_entity")
public class PackageLevelBagEntity {
	@Id
	private Integer id;

	@ElementCollection
	private List<String> names;
}
