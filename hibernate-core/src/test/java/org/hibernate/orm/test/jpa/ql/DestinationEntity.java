/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;

/**
 * @author Janario Oliveira
 */
@Entity
@Table(name = "destination_entity")
@NamedNativeQueries({
		@NamedNativeQuery(name = "DestinationEntity.insertSelect", query = "insert into destination_entity(id, from_id, fullNameFrom) "
				+ " select fe.id, fe.id, fe.name||fe.lastName from from_entity fe where fe.id in (:ids)"),
		@NamedNativeQuery(name = "DestinationEntity.insert", query = "insert into destination_entity(id, from_id, fullNameFrom) "
				+ "values (:generatedId, :fromId, :fullName)"),
		@NamedNativeQuery(name = "DestinationEntity.update", query = "update destination_entity set from_id=:idFrom, fullNameFrom=:fullName"
				+ " where id in (:ids)"),
		@NamedNativeQuery(name = "DestinationEntity.delete", query = "delete from destination_entity where id in (:ids)"),
		@NamedNativeQuery(name = "DestinationEntity.selectIds", query = "select id, from_id, fullNameFrom from destination_entity where id in (:ids) order by id") })
public class DestinationEntity {

	@Id
	@GeneratedValue
	Integer id;
	@ManyToOne(optional = false)
	@JoinColumn(name = "from_id")
	FromEntity from;
	@Column(nullable = false)
	String fullNameFrom;

	public DestinationEntity() {
	}

	public DestinationEntity(FromEntity from, String fullNameFrom) {
		this.from = from;
		this.fullNameFrom = fullNameFrom;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( from == null ) ? 0 : from.hashCode() );
		result = prime * result + ( ( fullNameFrom == null ) ? 0 : fullNameFrom.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		DestinationEntity other = (DestinationEntity) obj;
		if ( from == null ) {
			if ( other.from != null )
				return false;
		}
		else if ( !from.equals( other.from ) )
			return false;
		if ( fullNameFrom == null ) {
			if ( other.fullNameFrom != null )
				return false;
		}
		else if ( !fullNameFrom.equals( other.fullNameFrom ) )
			return false;
		return true;
	}

}
