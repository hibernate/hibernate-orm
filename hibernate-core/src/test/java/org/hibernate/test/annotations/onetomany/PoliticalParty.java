/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.onetomany;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class PoliticalParty {
	private String name;
	private Set<Politician> politicians = new HashSet<Politician>();

	@Id
	@Column(columnDefinition = "VARCHAR(60)")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
	public Set<Politician> getPoliticians() {
		return politicians;
	}

	public void setPoliticians(Set<Politician> politicians) {
		this.politicians = politicians;
	}

	public void addPolitician(Politician politician) {
		politicians.add( politician );
		politician.setParty( this );
	}
}
