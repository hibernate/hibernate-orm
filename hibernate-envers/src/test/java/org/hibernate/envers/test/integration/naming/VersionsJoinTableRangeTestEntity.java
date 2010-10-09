package org.hibernate.envers.test.integration.naming;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Concrete implementation of mapped superclass for Audit join table test.
 * 
 * @author Erik-Berndt Scheper
 * @see VersionsJoinTableRangeTestAlternateEntity
 * @see VersionsJoinTableRangeTestEntitySuperClass
 */
@Entity
@Table(name = "RANGE_TEST_ENTITY")
@org.hibernate.envers.Audited
public class VersionsJoinTableRangeTestEntity extends
		VersionsJoinTableRangeTestEntitySuperClass {

	private String value;

	/**
	 * Default constructor
	 */
	public VersionsJoinTableRangeTestEntity() {
		super();
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		VersionsJoinTableRangeTestEntity other = (VersionsJoinTableRangeTestEntity) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();

		output.append("VersionsJoinTableRangeComponentTestEntity {");
		output.append(" id = \"").append(getId()).append("\", ");
		output.append(" genericValue = \"").append(getGenericValue()).append(
				"\", ");
		output.append(" value = \"").append(this.value).append("\"}");
		return output.toString();
	}

}
