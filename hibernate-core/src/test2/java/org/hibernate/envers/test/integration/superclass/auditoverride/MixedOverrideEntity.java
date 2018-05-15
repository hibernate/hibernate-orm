/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.Entity;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
@AuditOverrides({
						@AuditOverride(forClass = AuditedBaseEntity.class, isAudited = false),
						@AuditOverride(forClass = AuditedBaseEntity.class, name = "number1", isAudited = true)
				})
public class MixedOverrideEntity extends AuditedBaseEntity {
	private String str2;

	public MixedOverrideEntity() {
	}

	public MixedOverrideEntity(String str1, Integer number, String str2) {
		super( str1, number );
		this.str2 = str2;
	}

	public MixedOverrideEntity(String str1, Integer number, Integer id, String str2) {
		super( str1, number, id );
		this.str2 = str2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MixedOverrideEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		MixedOverrideEntity that = (MixedOverrideEntity) o;

		if ( str2 != null ? !str2.equals( that.str2 ) : that.str2 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (str2 != null ? str2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "MixedOverrideEntity(" + super.toString() + ", str2 = " + str2 + ")";
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}
}
