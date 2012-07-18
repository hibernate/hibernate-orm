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
package org.hibernate.jpa.test.criteria;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

/**
 * An entity with multiple attributes of basic type for use in testing using those types/attributes
 * in queries.
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name="ENT_W_MANY_COLS")
public class MultiTypedBasicAttributesEntity {
	@Id
	@GeneratedValue( generator = "increment" )
	@GenericGenerator( name = "increment", strategy = "increment" )
	private Long id;
	private byte[] someBytes;
	private Byte[] someWrappedBytes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public byte[] getSomeBytes() {
		return someBytes;
	}

	public void setSomeBytes(byte[] someBytes) {
		this.someBytes = someBytes;
	}

	public Byte[] getSomeWrappedBytes() {
		return someWrappedBytes;
	}

	public void setSomeWrappedBytes(Byte[] someWrappedBytes) {
		this.someWrappedBytes = someWrappedBytes;
	}
}
