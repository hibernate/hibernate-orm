/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.crossschema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */

@Entity
@Table(schema = "SCHEMA1")
public class SchemaOneEntity {
	@Id
	private String id;
}
