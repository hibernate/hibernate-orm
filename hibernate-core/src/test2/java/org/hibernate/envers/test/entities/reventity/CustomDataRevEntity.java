/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.reventity;

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
public class CustomDataRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private int customId;

	@RevisionTimestamp
	private long customTimestamp;

	private String data;

	public int getCustomId() {
		return customId;
	}

	public void setCustomId(int customId) {
		this.customId = customId;
	}

	public long getCustomTimestamp() {
		return customTimestamp;
	}

	public void setCustomTimestamp(long customTimestamp) {
		this.customTimestamp = customTimestamp;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof CustomDataRevEntity) ) {
			return false;
		}

		CustomDataRevEntity that = (CustomDataRevEntity) o;

		if ( customId != that.customId ) {
			return false;
		}
		if ( customTimestamp != that.customTimestamp ) {
			return false;
		}
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = customId;
		result = 31 * result + (int) (customTimestamp ^ (customTimestamp >>> 32));
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}