/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test.client;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class AuditedDataPoint {
	@Id
	@GeneratedValue
	private Integer id;
	private String name;

	AuditedDataPoint() {

	}

	public AuditedDataPoint(String name) {
		this( null, name );
	}

	public AuditedDataPoint(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		int result;
		result = ( id != null ? id.hashCode() : 0 );
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !( obj instanceof AuditedDataPoint ) ) {
			return false;
		}
		AuditedDataPoint that = (AuditedDataPoint) obj;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		return true;
	}
}
