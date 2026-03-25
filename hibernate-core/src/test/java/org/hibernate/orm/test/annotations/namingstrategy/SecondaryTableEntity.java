/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity(name = "SecondaryTableEntity")
@Table
@SecondaryTable(
		name = "context_details",
		uniqueConstraints = @UniqueConstraint(columnNames = "secondaryValue")
)
public class SecondaryTableEntity {
	@Id
	private Integer id;

	@Column(table = "context_details")
	private String secondaryValue;
}
