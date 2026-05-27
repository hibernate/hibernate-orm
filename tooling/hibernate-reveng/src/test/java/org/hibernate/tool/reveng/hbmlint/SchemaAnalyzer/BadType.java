/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbmlint.SchemaAnalyzer;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name = "BAD_TYPE")
public class BadType {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "does_not_exist")
	@TableGenerator(name = "does_not_exist", table = "does_not_exist")
	private int id;

	@Lob
	private String name;
}
