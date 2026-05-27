/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.SchemaAnalyzer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name = "MISSING_TABLE")
public class MissingTable {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "HILO_TABLE")
	@TableGenerator(name = "HILO_TABLE", table = "HILO_TABLE")
	private long id;
}
