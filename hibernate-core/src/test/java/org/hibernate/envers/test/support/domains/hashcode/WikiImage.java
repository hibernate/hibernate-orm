/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.hashcode;

import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class WikiImage {
	@Id
	@GeneratedValue
	private Long id;

	@Basic
	private String name;

	public WikiImage() {
	}

	public WikiImage(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		WikiImage wikiImage = (WikiImage) o;
		return Objects.equals( name, wikiImage.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name );
	}

	@Override
	public String toString() {
		return "WikiImage{" +
				"name='" + name + '\'' +
				'}';
	}
}
