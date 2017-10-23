/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 */
@Entity
@Table(name = "NotAuditedSubclass")
public class NotAuditedSubclassEntity extends AuditedAllMappedSuperclass {
	@Id
	@GeneratedValue
	private Integer id;

	private String notAuditedStr;

	public NotAuditedSubclassEntity() {
	}

	public NotAuditedSubclassEntity(Integer id, String str, String otherStr, String notAuditedStr) {
		super( str, otherStr );
		this.notAuditedStr = notAuditedStr;
		this.id = id;
	}

	public NotAuditedSubclassEntity(String str, String otherStr, String notAuditedStr) {
		super( str, otherStr );
		this.notAuditedStr = notAuditedStr;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNotAuditedStr() {
		return notAuditedStr;
	}

	public void setNotAuditedStr(String notAuditedStr) {
		this.notAuditedStr = notAuditedStr;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotAuditedSubclassEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		NotAuditedSubclassEntity that = (NotAuditedSubclassEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (id != null ? id.hashCode() : 0);
		return result;
	}
}
