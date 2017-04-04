/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded.many2one;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * THe entity target of the many-to-one from a component/embeddable.
 *
 * @author Steve Ebersole
 */
@Entity
public class Country implements Serializable {
	private String iso2;
	private String name;

	public Country() {
	}

	public Country(String iso2, String name) {
		this.iso2 = iso2;
		this.name = name;
	}

	@Id
	public String getIso2() {
		return iso2;
	}

	public void setIso2(String iso2) {
		this.iso2 = iso2;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
