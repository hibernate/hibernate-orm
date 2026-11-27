package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity(name = "AuditedJoinedChild")
@Table
public class AuditedJoinedChild extends AuditedJoinedParent {
	private String childValue;
}
