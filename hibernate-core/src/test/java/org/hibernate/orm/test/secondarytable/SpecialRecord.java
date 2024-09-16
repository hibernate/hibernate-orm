/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
