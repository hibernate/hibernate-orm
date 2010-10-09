// $Id: GeneratedPropertyEntity.java 7800 2005-08-10 12:13:00Z steveebersole $
package org.hibernate.test.generated;

/**
 * Implementation of GeneratedPropertyEntity.
 *
 * @author Steve Ebersole
 */
public class GeneratedPropertyEntity {
	private Long id;
	private String name;
	private byte[] lastModified;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public byte[] getLastModified() {
		return lastModified;
	}

	public void setLastModified(byte[] lastModified) {
		this.lastModified = lastModified;
	}
}
