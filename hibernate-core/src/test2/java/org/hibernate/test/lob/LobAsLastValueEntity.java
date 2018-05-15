/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Chris Cranford
 */
public class LobAsLastValueEntity implements Serializable {
	private Integer id;
	private String name;
	private String details;
	private String title;

	public LobAsLastValueEntity() {

	}

	public LobAsLastValueEntity(String name, String details, String title) {
		this.name = name;
		this.details = details;
		this.title = title;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		LobAsLastValueEntity that = (LobAsLastValueEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( details, that.details ) &&
				Objects.equals( title, that.title );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, details, title );
	}
}
