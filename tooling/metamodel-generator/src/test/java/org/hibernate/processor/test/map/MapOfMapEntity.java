package org.hibernate.processor.test.map;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table( name = "MAP_OF_MAP_ENTITY" )
public class MapOfMapEntity {
	@Id
	@Column(name="key_")
	private String key;

	@JdbcTypeCode(SqlTypes.JSON)
	private Map<String, Map<String, Object>> mapOfMap;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
