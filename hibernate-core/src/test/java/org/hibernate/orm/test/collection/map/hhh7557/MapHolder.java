/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map.hhh7557;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.Table;
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
