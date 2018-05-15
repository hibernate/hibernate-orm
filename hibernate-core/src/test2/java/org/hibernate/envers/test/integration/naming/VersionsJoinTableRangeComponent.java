/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.FetchMode;

/**
 * An embeddable component containing a list of
 * {@link VersionsJoinTableRangeTestEntitySuperClass}-instances
 *
 * @param <T>
 *
 * @author Erik-Berndt Scheper
 */
@Embeddable
public final class VersionsJoinTableRangeComponent<T extends VersionsJoinTableRangeTestEntitySuperClass> {

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@org.hibernate.annotations.Fetch(value = FetchMode.SUBSELECT)
	@org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@JoinColumn(name = "VJTRCTE_ID", insertable = true, updatable = false, nullable = false)
//	Note:	If this is processed without override annotation, then we should get a 
//			org.hibernate.DuplicateMappingException: 
//			Duplicate class/entity mapping JOIN_TABLE_COMPONENT_1_AUD
	@org.hibernate.envers.AuditJoinTable(name = "JOIN_TABLE_COMPONENT_1_AUD",
										 inverseJoinColumns = @JoinColumn(name = "VJTRTE_ID"))
	private List<T> range = new ArrayList<T>();

	// ********************** Accessor Methods ********************** //

	protected List<T> getRange() {
		return this.range;
	}

	// ********************** Common Methods ********************** //

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((range == null) ? 0 : range.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		VersionsJoinTableRangeComponent<?> other = (VersionsJoinTableRangeComponent<?>) obj;
		if ( range == null ) {
			if ( other.range != null ) {
				return false;
			}
		}
		else if ( !range.equals( other.range ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();

		output.append( "VersionsJoinTableRangeComponent {" );
		output.append( " range = \"" );
		for ( T instance : range ) {
			output.append( instance ).append( "\n" );
		}
		output.append( "\"}" );

		return output.toString();
	}

}
