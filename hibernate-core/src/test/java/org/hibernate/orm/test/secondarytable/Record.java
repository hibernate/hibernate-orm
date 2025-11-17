/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.secondarytable;

import org.hibernate.annotations.SecondaryRow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "Details")
@SecondaryTable(name = "NonOptional")
@SecondaryTable(name = "Optional")
@SecondaryRow(table = "NonOptional", optional = false)
@SecondaryRow(table = "Optional", optional = true)
@SequenceGenerator(name="RecordSeq", sequenceName = "RecordId", allocationSize = 1)
public class Record {
	@Id @GeneratedValue(generator = "RecordSeq")  long id;
	String name;
	@Column(table = "NonOptional") String text;
	@Column(table = "NonOptional") boolean enabled;
	@Column(table = "Optional", name="`comment`") String comment;
}
