/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.list.pkg;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "PackageLevelListEntity")
@Table(name = "pkg_list_entity")
public class PackageLevelListEntity {
	@Id
	private Integer id;

	@ElementCollection
	private List<String> names;
}
