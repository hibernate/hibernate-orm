/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity
@AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
public class SubclassWithUuidAsId extends MappedSuperClassWithUuidAsBasic {

	@Id
	@Access(AccessType.PROPERTY)
	@Override
	public Long getUid() {
		return super.getUid();
	}
}
