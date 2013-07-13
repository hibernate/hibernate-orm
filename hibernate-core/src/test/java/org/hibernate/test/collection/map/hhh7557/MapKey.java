/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.collection.map.hhh7557;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.jboss.logging.Logger;

/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
@Entity
@Table(name = "map_key", uniqueConstraints = {
		@UniqueConstraint(columnNames = {"name", "default_map_value_id"})
})
public class MapKey {
	private static final Logger log = Logger.getLogger( MapKey.class );

	private Long id;
	private String name;
	private MapValue defaultValue;

	public MapKey() {
	}

	public MapKey(String name, MapValue defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}

	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "name", nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		log.tracef( "Setting name : %s", name );
		this.name = name;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "default_map_value_id", nullable = false)
	public MapValue getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(MapValue defaultValue) {
		log.tracef( "Setting defaultValue : %s", defaultValue );
		this.defaultValue = defaultValue;
	}

	private int previousHashCode = -1;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getDefaultValue() == null) ? 0 : getDefaultValue().hashCode());
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		log.tracef(
				"Calculated hashcode [%s] = %s (previous=%s, changed?=%s)",
				this,
				result,
				previousHashCode,
				!(previousHashCode == -1 || previousHashCode == result)
		);
		previousHashCode = result;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		log.tracef( "Checking equality : %s -> %s", this, obj );
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof MapKey) ) {
			return false;
		}
		MapKey other = (MapKey) obj;
		if ( getDefaultValue() == null ) {
			if ( other.getDefaultValue() != null ) {
				return false;
			}
		}
		else if ( !getDefaultValue().equals( other.getDefaultValue() ) ) {
			return false;
		}
		if ( getName() == null ) {
			if ( other.getName() != null ) {
				return false;
			}
		}
		else if ( !getName().equals( other.getName() ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "MapKey [id=" ).append( getId() )
				.append( ", name=" ).append( getName() )
				.append( ", defaultValue=" ).append( getDefaultValue() )
				.append( "]" );
		return builder.toString();
	}

}
