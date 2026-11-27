package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.annotations.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "AuditedEntity")
@Table
@Audited
@Audited.Table(changesetIdColumn = "txn_id", modificationTypeColumn = "mod_type")
public class AuditedEntity {
	@Id
	private Integer id;
}
