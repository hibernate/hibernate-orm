/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.crossschema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(schema = "SCHEMA2")
public class SchemaTwoEntity {

	@Id
	private String id;

	@OneToMany
	@JoinColumn
	private Set<SchemaOneEntity> schemaOneEntities = new HashSet<SchemaOneEntity>();
}
