/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema.pkg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "PackageLevelDefaultSchemaEntity")
public class PackageLevelDefaultSchemaEntity {
	@Id
	public Long id;
}
