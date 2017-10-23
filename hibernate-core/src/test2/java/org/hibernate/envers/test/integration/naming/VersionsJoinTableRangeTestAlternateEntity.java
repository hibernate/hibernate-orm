/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Alternate implementation of mapped superclass for Audit join table test.
 *
 * @author Erik-Berndt Scheper
 * @see VersionsJoinTableRangeTestEntity
 * @see VersionsJoinTableRangeTestEntitySuperClass
 */
@Entity
@Table(name = "RANGE_TEST_ALTERNATE_ENT")
@org.hibernate.envers.Audited
public class VersionsJoinTableRangeTestAlternateEntity extends
													   VersionsJoinTableRangeTestEntitySuperClass {

	private String alternateValue;

	/**
	 * Default constructor
	 */
	public VersionsJoinTableRangeTestAlternateEntity() {
		super();
	}

	/**
	 * @return the alternateValue
	 */
	public String getAlternateValue() {
		return alternateValue;
	}

	/**
	 * @param alternateValue the alternateValue to set
	 */
	public void setAlternateValue(String alternateValue) {
		this.alternateValue = alternateValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((alternateValue == null) ? 0 : alternateValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals( obj ) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		VersionsJoinTableRangeTestAlternateEntity other = (VersionsJoinTableRangeTestAlternateEntity) obj;
		if ( alternateValue == null ) {
			if ( other.alternateValue != null ) {
				return false;
			}
		}
		else if ( !alternateValue.equals( other.alternateValue ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();

		output.append( "VersionsJoinTableRangeComponentTestEntity {" );
		output.append( " id = \"" ).append( getId() ).append( "\", " );
		output.append( " genericValue = \"" ).append( getGenericValue() ).append(
				"\", "
		);
		output.append( " alternateValue = \"" ).append( this.alternateValue )
				.append( "\"}" );
		return output.toString();
	}

}
