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
