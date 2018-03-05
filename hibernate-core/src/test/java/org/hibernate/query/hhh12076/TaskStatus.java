/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12076;

import java.util.Date;

public class TaskStatus {

	private Long _id;
	private Integer _version;
	private Date _creationDate;
	private Date _modifiedDate;

	private boolean _active;
	private Integer _orderIndex;

	private String _name;
	private String _displayName;

	public TaskStatus() {
	}

	public String getEntityName() {
		return _displayName;
	}

	public Long getId() {
		return _id;
	}

	public void setId(Long id) {
		_id = id;
	}

	public Integer getVersion() {
		return _version;
	}

	public void setVersion(Integer version) {
		_version = version;
	}

	public Date getCreationDate() {
		return _creationDate;
	}

	public void setCreationDate(Date creationDate) {
		_creationDate = creationDate;
	}

	public Date getModifiedDate() {
		return _modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		_modifiedDate = modifiedDate;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getDisplayName() {
		return _displayName;
	}

	public void setDisplayName(String displayName) {
		_displayName = displayName;
	}

	public boolean isActive() {
		return _active;
	}

	public void setActive(boolean active) {
		_active = active;
	}

	@Override
	public String toString() {
		return _name;
	}

	public Integer getOrderIndex() {
		return _orderIndex;
	}

	public void setOrderIndex(Integer ordering) {
		_orderIndex = ordering;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( _name == null ) ? 0 : _name.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) {
			return false;
		}
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof TaskStatus ) ) {
			return false;
		}
		TaskStatus other = (TaskStatus) obj;
		if ( _name == null ) {
			if ( other._name != null ) {
				return false;
			}
		}
		else if ( !_name.equals( other._name ) ) {
			return false;
		}
		return true;
	}

}
