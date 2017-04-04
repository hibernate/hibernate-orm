/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.norevision;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

@Audited
@Entity
public class Person implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;
	@AuditMappedBy(mappedBy = "person")
	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "person_id")
	private Set<Name> names;

	public Person() {
		names = new HashSet<Name>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Name> getNames() {
		return names;
	}

	public void setNames(Set<Name> names) {
		this.names = names;
	}
}
