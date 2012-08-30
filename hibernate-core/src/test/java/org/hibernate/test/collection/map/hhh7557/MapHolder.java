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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
@Entity
@Table(name = "map_holder")
public class MapHolder {
	private Long id;
	private Map<MapKey, MapValue> map;

	@Id
	@GeneratedValue
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToMany
	@JoinTable(
			name = "map_key_map_value",
			joinColumns = @JoinColumn(name = "map_holder_id", nullable = false),
			inverseJoinColumns = @JoinColumn(name = "map_value_id", nullable = false)
	)
	@MapKeyJoinColumn(name = "map_key_id", nullable = false)
	public Map<MapKey, MapValue> getMap() {
		return map;
	}

	public void setMap(Map<MapKey, MapValue> map) {
		this.map = map;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "MapHolder [id=" ).append( id ).append( "]" );
		return builder.toString();
	}

}
