/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.jpa.ql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Janario Oliveira
 */
@Entity
@Table(name = "from_entity")
public class FromEntity {

	@Id
	@GeneratedValue
	Integer id;
	@Column(nullable = false)
	String name;
	@Column(nullable = false)
	String lastName;

	public FromEntity() {
	}

	public FromEntity(String name, String lastName) {
		this.name = name;
		this.lastName = lastName;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + ( this.name != null ? this.name.hashCode() : 0 );
		hash = 53 * hash + ( this.lastName != null ? this.lastName.hashCode() : 0 );
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final FromEntity other = (FromEntity) obj;
		if ( ( this.name == null ) ? ( other.name != null ) : !this.name.equals( other.name ) ) {
			return false;
		}
		if ( ( this.lastName == null ) ? ( other.lastName != null ) : !this.lastName.equals( other.lastName ) ) {
			return false;
		}
		return true;
	}
}
