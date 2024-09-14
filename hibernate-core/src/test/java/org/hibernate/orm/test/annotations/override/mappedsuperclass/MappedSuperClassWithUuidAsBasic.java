/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
@Access(AccessType.FIELD)
public class MappedSuperClassWithUuidAsBasic {

	Long uid;

	public Long getUid() {
		return uid;
	}

	public void setUid(Long uid) {
		this.uid = uid;
	}
}
