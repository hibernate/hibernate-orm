// $Id: $
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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