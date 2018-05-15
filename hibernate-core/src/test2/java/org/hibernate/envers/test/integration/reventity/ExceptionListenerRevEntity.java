/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

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
@RevisionEntity(TestExceptionRevisionListener.class)
public class ExceptionListenerRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private int id;

	@RevisionTimestamp
	private long timestamp;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ExceptionListenerRevEntity) ) {
			return false;
		}

		ExceptionListenerRevEntity revEntity = (ExceptionListenerRevEntity) o;

		if ( id != revEntity.id ) {
			return false;
		}
		if ( timestamp != revEntity.timestamp ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}
}