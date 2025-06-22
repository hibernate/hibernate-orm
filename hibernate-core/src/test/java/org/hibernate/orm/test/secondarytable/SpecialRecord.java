/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.secondarytable;

import java.time.Instant;

import org.hibernate.annotations.SecondaryRow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

@Entity
@SecondaryTable(name = "`View`")
@SecondaryRow(table = "`View`", owned = false)
public class SpecialRecord extends Record {
	@Column(table = "`View`", name="`timestamp`")
	Instant timestamp;
}
