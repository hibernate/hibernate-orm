package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.MappedSuperclass;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
@AuditOverrides({
						@AuditOverride(forClass = BaseEntity.class, name = "str1", isAudited = false),
						@AuditOverride(forClass = BaseEntity.class, name = "number1", isAudited = true)
				})
public class ExtendedBaseEntity extends BaseEntity {
	@Audited
	private String str2;

	@NotAudited
	private Integer number2;

	public ExtendedBaseEntity() {
	}

	public ExtendedBaseEntity(String str1, Integer number1, Integer id, String str2, Integer number2) {
		super( str1, number1, id );
		this.str2 = str2;
		this.number2 = number2;
	}

	public ExtendedBaseEntity(String str1, Integer number1, String str2, Integer number2) {
		super( str1, number1 );
		this.str2 = str2;
		this.number2 = number2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ExtendedBaseEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ExtendedBaseEntity that = (ExtendedBaseEntity) o;

		if ( number2 != null ? !number2.equals( that.number2 ) : that.number2 != null ) {
			return false;
		}
		if ( str2 != null ? !str2.equals( that.str2 ) : that.str2 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (str2 != null ? str2.hashCode() : 0);
		result = 31 * result + (number2 != null ? number2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ExtendedBaseEntity(" + super.toString() + ", str2 = " + str2 + ", number2 = " + number2 + ")";
	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}

	public Integer getNumber2() {
		return number2;
	}

	public void setNumber2(Integer number2) {
		this.number2 = number2;
	}
}
