/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
