/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Gail Badner
 */
@Entity
public class ManyToOneEntity {
	@Id
	private Long id;
	private String theName;
	@ManyToOne
	SimpleEntity simpleEntity;

	public ManyToOneEntity() {
	}

	public ManyToOneEntity(String name) {
		this.theName = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return theName;
	}

	public void setName(String name) {
		this.theName = name;
	}

	public SimpleEntity getSimpleEntity() {
		return simpleEntity;
	}

	public void setSimpleEntity(SimpleEntity simpleEntity) {
		this.simpleEntity = simpleEntity;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityWithManyToOne" );
		sb.append( "{id=" ).append( id );
		sb.append( ", theName='" ).append( theName ).append( '\'' );
		sb.append( ", simpleEntity=" ).append( simpleEntity );
		sb.append( '}' );
		return sb.toString();
	}
}
