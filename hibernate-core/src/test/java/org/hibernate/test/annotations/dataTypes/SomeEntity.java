/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.dataTypes;

import java.util.Date;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "SOMEENTITY")
@Access(AccessType.FIELD)
public class SomeEntity {
	@Id
	@Temporal(TemporalType.DATE)
	@Column(name = "ID")
	private java.util.Date id;
	@Column(name = "TIMEDATA")
	private java.sql.Time timeData;
	@Column(name = "TSDATA")
	private java.sql.Timestamp tsData;
	@Lob
	private Byte[] byteData;
	private Character[] charData;

	public SomeEntity() {
	}

	public SomeEntity(Date id) {
		this.id = id;
	}

	public java.util.Date getId() {
		return id;
	}

	public void setId(java.util.Date id) {
		this.id = id;
	}

	public Character[] getCharData() {
		return charData;
	}

	public void setCharData(Character[] charData) {
		this.charData = charData;
	}

	public java.sql.Time getTimeData() {
		return timeData;
	}

	public void setTimeData(java.sql.Time timeData) {
		this.timeData = timeData;
	}

	public java.sql.Timestamp getTsData() {
		return tsData;
	}

	public void setTsData(java.sql.Timestamp tsData) {
		this.tsData = tsData;
	}

	public Byte[] getByteData() {
		return byteData;
	}

	public void setByteData(Byte[] byteData) {
		this.byteData = byteData;
	}
}
