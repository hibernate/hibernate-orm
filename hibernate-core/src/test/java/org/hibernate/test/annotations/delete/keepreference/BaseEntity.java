/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.delete.keepreference;

import javax.persistence.ColumnResult;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SqlResultSetMapping;

/**
 * @author Richard Bizik
 */
@MappedSuperclass
@SqlResultSetMapping(
		name = "deleted_selection",
		columns = @ColumnResult( name = "deleted", type = Boolean.class )
)
public class BaseEntity {

	@Id
	private Integer id;
	private boolean deleted;

	protected BaseEntity() {
	}

	public BaseEntity(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	private void setId(Integer id) {
		this.id = id;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
