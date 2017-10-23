/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

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
