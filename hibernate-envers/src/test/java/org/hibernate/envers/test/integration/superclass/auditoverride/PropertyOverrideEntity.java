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
						@AuditOverride(forClass = BaseEntity.class, name = "str1", isAudited = false),
						@AuditOverride(forClass = BaseEntity.class, name = "number1", isAudited = true)
				})
public class PropertyOverrideEntity extends BaseEntity {
	private String str2;

	public PropertyOverrideEntity() {
	}

	public PropertyOverrideEntity(String str1, Integer number1, String str2) {
		super( str1, number1 );
		this.str2 = str2;
	}

	public PropertyOverrideEntity(String str1, Integer number1, Integer id, String str2) {
		super( str1, number1, id );
		this.str2 = str2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PropertyOverrideEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		PropertyOverrideEntity that = (PropertyOverrideEntity) o;

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
		return "PropertyOverrideEntity(" + super.toString() + ", str2 = " + str2 + ")";
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}
}
