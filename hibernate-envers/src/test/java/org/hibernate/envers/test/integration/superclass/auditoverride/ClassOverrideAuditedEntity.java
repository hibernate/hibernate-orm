package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz.antoniak at gmail dot com)
 */
@Entity
@Table(name = "ClassOverrideAudited")
@AuditOverrides({@AuditOverride(forClass = NotAuditedBaseEntity.class, isAudited = true)})
public class ClassOverrideAuditedEntity extends NotAuditedBaseEntity {
	@Audited
	private String str2;

	public ClassOverrideAuditedEntity() {
	}

	public ClassOverrideAuditedEntity(String str1, Integer number, String str2) {
		super( str1, number );
		this.str2 = str2;
	}

	public ClassOverrideAuditedEntity(String str1, Integer number, Integer id, String str2) {
		super( str1, number, id );
		this.str2 = str2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ClassOverrideAuditedEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ClassOverrideAuditedEntity that = (ClassOverrideAuditedEntity) o;

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
		return "ClassOverrideAuditedEntity(" + super.toString() + ", str2 = " + str2 + ")";
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}
}
