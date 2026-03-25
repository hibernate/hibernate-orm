package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.annotations.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "AuditedItem")
@Table
@Audited
public class AuditedItem {
	@Id
	private Integer id;
}
