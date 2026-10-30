/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

import org.hibernate.envers.Audited;

/**
 * Test entity for HHH-20926.
 * Tests that MapCollectionMapper correctly uses getIndexType() instead of getKeyType()
 * when comparing map keys in audited collections.
 *
 * The entity ID type (BigDecimal) differs from the map key type (String),
 * which exposes the bug when MapCollectionMapper incorrectly uses getKeyType()
 * (which returns the foreign-key type referencing the collection owner)
 * instead of getIndexType() (which returns the actual map key type).
 *
 * @author Chikumar
 */
@Entity
@Audited
public class BigDecimalMapEntity {
	@Id
	private BigDecimal id;

	@ElementCollection
	@CollectionTable(
			name = "bigdecimal_map_entry",
			joinColumns = @JoinColumn(name = "entity_id")
	)
	@MapKeyColumn(name = "entry_key", nullable = false)
	@Column(name = "entry_value", nullable = false)
	private Map<String, String> entries = new HashMap<>();

	public BigDecimalMapEntity() {
	}

	public BigDecimalMapEntity(BigDecimal id) {
		this.id = id;
	}

	public BigDecimal getId() {
		return id;
	}

	public void setId(BigDecimal id) {
		this.id = id;
	}

	public Map<String, String> getEntries() {
		return entries;
	}

	public void setEntries(Map<String, String> entries) {
		this.entries = entries;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BigDecimalMapEntity) ) {
			return false;
		}

		BigDecimalMapEntity that = (BigDecimalMapEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	@Override
	public String toString() {
		return "BigDecimalMapEntity(id = " + id + ", entries = " + entries + ")";
	}
}
