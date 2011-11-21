package org.hibernate.test.criteria;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
@Entity
@IdClass(ListActionRole.class)
public class ListActionRole extends VersionedRecord {
	@Id
	@Enumerated(EnumType.STRING)
	Code roleCode;

	@ManyToOne(targetEntity = Role.class)
	@JoinColumn(nullable = false)
	Role role;

	@Override
	public String toString() {
		return "ListActionRole.Id(roleCode=" + roleCode + ")";
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}
}
