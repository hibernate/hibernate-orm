/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
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
