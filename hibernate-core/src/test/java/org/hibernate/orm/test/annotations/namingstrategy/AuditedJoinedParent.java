package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.List;

import org.hibernate.annotations.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity(name = "AuditedJoinedParent")
@Table
@Audited
@Inheritance(strategy = InheritanceType.JOINED)
public class AuditedJoinedParent {
	@Id
	private Integer id;

	@OneToMany
	@JoinColumn(name = "parent_fk")
	private List<AuditedItem> items;
}
