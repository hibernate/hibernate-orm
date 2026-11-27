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
