/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob.locator;

import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class LobHolder {
	@Id
	@GeneratedValue
	private Long id;

	private Clob clobLocator;

	private Blob blobLocator;

	private Integer counter;

	public LobHolder() {
	}

	public LobHolder(Blob blobLocator, Clob clobLocator, Integer counter) {
		this.blobLocator = blobLocator;
		this.clobLocator = clobLocator;
		this.counter = counter;
	}

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

	public Integer getCounter() {
		return counter;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}
}
