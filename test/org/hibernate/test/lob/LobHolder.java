//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.test.lob;

import java.io.Serializable;
import java.sql.Clob;
import java.sql.Blob;

/**
 * An entity containing all kinds of good LOB-type data...
 * <p/>
 * {@link #serialData} is used to hold general serializable data which is
 * mapped via the {@link org.hibernate.type.SerializableType}.
 * <p/>
 * {@link #materializedClob} is used to hold CLOB data that is materialized
 * into a String immediately; it is mapped via the
 * {@link org.hibernate.type.TextType}.
 * <p/>
 * {@link #clobLocator} is used to hold CLOB data that is materialized lazily
 * via a JDBC CLOB locator; it is mapped via the
 * {@link org.hibernate.type.ClobType}
 * <p/>
 * {@link #materializedBlob} is used to hold BLOB data that is materialized
 * into a byte array immediately; it is mapped via the
 * {@link org.hibernate.test.lob.MaterializedBlobType}.
 * <p/>
 * {@link #blobLocator} is used to hold BLOB data that is materialized lazily
 * via a JDBC BLOB locator; it is mapped via the
 * {@link org.hibernate.type.BlobType}
 * 
 *
 * @author Steve Ebersole
 */
public class LobHolder {
	private Long id;

	private Serializable serialData;

	private String materializedClob;
	private Clob clobLocator;

	private byte[] materializedBlob;
	private Blob blobLocator;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Serializable getSerialData() {
		return serialData;
	}

	public void setSerialData(Serializable serialData) {
		this.serialData = serialData;
	}

	public String getMaterializedClob() {
		return materializedClob;
	}

	public void setMaterializedClob(String materializedClob) {
		this.materializedClob = materializedClob;
	}

	public Clob getClobLocator() {
		return clobLocator;
	}

	public void setClobLocator(Clob clobLocator) {
		this.clobLocator = clobLocator;
	}

	public byte[] getMaterializedBlob() {
		return materializedBlob;
	}

	public void setMaterializedBlob(byte[] materializedBlob) {
		this.materializedBlob = materializedBlob;
	}

	public Blob getBlobLocator() {
		return blobLocator;
	}

	public void setBlobLocator(Blob blobLocator) {
		this.blobLocator = blobLocator;
	}
}
