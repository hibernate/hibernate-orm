/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: $

package org.hibernate.test.annotations.idclass.xml;
import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class HabitatSpeciesLink implements Serializable {
	private static final long serialVersionUID = -7079021236893433038L;

	private Long habitatId;

	private Long speciesId;

	public Long getHabitatId() {
		return this.habitatId;
	}

	public void setHabitatId(Long newHabitatId) {
		this.habitatId = newHabitatId;
	}

	public Long getSpeciesId() {
		return this.speciesId;
	}

	public void setSpeciesId(Long newSpeciesId) {
		this.speciesId = newSpeciesId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.getHabitatId() == null ) ? 0
				: this.getHabitatId().hashCode() );
		result = prime * result + ( ( this.getSpeciesId() == null ) ? 0
				: this.getSpeciesId().hashCode() );
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
		if ( !( obj instanceof HabitatSpeciesLink ) ) {
			return false;
		}
		final HabitatSpeciesLink other = ( HabitatSpeciesLink ) obj;
		if ( this.getHabitatId() == null ) {
			if ( other.getHabitatId() != null ) {
				return false;
			}
		}
		else if ( !this.getHabitatId().equals( other.getHabitatId() ) ) {
			return false;
		}
		if ( this.getSpeciesId() == null ) {
			if ( other.getSpeciesId() != null ) {
				return false;
			}
		}
		else if ( !this.getSpeciesId().equals( other.getSpeciesId() ) ) {
			return false;
		}
		return true;
	}

	public static class HabitatSpeciesLinkId implements Serializable {
		private Long habitatId;

		private Long speciesId;

		public Long getHabitatId() {
			return this.habitatId;
		}

		public void setHabitatId(Long newHabitatId) {
			this.habitatId = newHabitatId;
		}

		public Long getSpeciesId() {
			return this.speciesId;
		}

		public void setSpeciesId(Long newSpeciesId) {
			this.speciesId = newSpeciesId;
		}

		/**
		 * Equality must be implemented in terms of identity field equality, and
		 * must use instanceof rather than comparing classes directly (some JPA
		 * implementations may subclass the identity class).
		 */
		public boolean equals(Object other) {
			if ( other == this ) {
				return true;
			}
			if ( !( other instanceof HabitatSpeciesLinkId ) ) {
				return false;
			}
			HabitatSpeciesLinkId mi = ( HabitatSpeciesLinkId ) other;
			return ( habitatId == mi.habitatId || ( habitatId != null && habitatId
					.equals( mi.habitatId ) ) )
					&& ( speciesId == mi.speciesId || ( speciesId != null && speciesId
					.equals( mi.speciesId ) ) );
		}

		/**
		 * Hashcode must also depend on identity values.
		 */
		public int hashCode() {
			return ( ( habitatId == null ) ? 0
					: habitatId.hashCode() ) ^ ( ( speciesId == null ) ? 0
					: speciesId.hashCode() );
		}

		public String toString() {
			return "habitatId[" + habitatId + "],speciesId[" + speciesId + "]";
		}
	}
}
