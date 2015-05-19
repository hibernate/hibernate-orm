/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.reventity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@GenericGenerator(name = "EnversTestingRevisionGenerator",
				  strategy = "org.hibernate.id.enhanced.TableGenerator",
				  parameters = {
						  @Parameter(name = "table_name", value = "REVISION_GENERATOR"),
						  @Parameter(name = "initial_value", value = "1"),
						  @Parameter(name = "increment_size", value = "1"),
						  @Parameter(name = "prefer_entity_table_as_segment_value", value = "true")
				  }
)
@RevisionEntity
public class CustomRevEntityColumnMapping {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@Column(columnDefinition = "int")
	@RevisionNumber
	private Long customId;

	@RevisionTimestamp
	private long customTimestamp;

	public Long getCustomId() {
		return customId;
	}

	public void setCustomId(Long customId) {
		this.customId = customId;
	}

	public long getCustomTimestamp() {
		return customTimestamp;
	}

	public void setCustomTimestamp(long customTimestamp) {
		this.customTimestamp = customTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CustomRevEntityColumnMapping that = (CustomRevEntityColumnMapping) o;

		if ( customTimestamp != that.customTimestamp ) {
			return false;
		}
		if ( customId != null ? !customId.equals( that.customId ) : that.customId != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = customId != null ? customId.hashCode() : 0;
		result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
		return result;
	}
}