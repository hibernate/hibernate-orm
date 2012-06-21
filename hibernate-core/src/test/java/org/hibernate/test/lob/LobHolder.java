package org.hibernate.test.lob;
import java.sql.Blob;
import java.sql.Clob;

/**
 * An entity containing all kinds of good LOB-type data...
 * <p/>
 * {@link #clobLocator} is used to hold CLOB data that is materialized lazily
 * via a JDBC CLOB locator; it is mapped via the
 * {@link org.hibernate.type.ClobType}
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

	private Clob clobLocator;

	private Blob blobLocator;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Clob getClobLocator() {
		return clobLocator;
	}

	public void setClobLocator(Clob clobLocator) {
		this.clobLocator = clobLocator;
	}

	public Blob getBlobLocator() {
		return blobLocator;
	}

	public void setBlobLocator(Blob blobLocator) {
		this.blobLocator = blobLocator;
	}
}
