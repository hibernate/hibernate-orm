/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
