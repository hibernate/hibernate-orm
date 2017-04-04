/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embedded.one2many;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

import org.hibernate.annotations.AccessType;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Embeddable
@AccessType("property")
public class PersonName extends Name {
	private Set<Alias> aliases = new HashSet<Alias>();

	public PersonName() {
	}

	public PersonName(String first, String last) {
		super( first, last );
	}

	@OneToMany( cascade = CascadeType.ALL )
	public Set<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(Set<Alias> aliases) {
		this.aliases = aliases;
	}
}
