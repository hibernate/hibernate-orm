/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
@Entity
// HHH-7732 -- "EntityWithAnElementCollection" is too long for Oracle.
@Table( name = "EWAEC" )
public class EntityWithAnElementCollection {
	private Long id;
	private Set<String> someStrings = new HashSet<String>();

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ElementCollection
	// HHH-7732 -- "EntityWithAnElementCollection_someStrings" is too long for Oracle.
	@JoinTable( 
			name = "SomeStrings", 
			joinColumns = @JoinColumn( name = "EWAEC_ID") )
	public Set<String> getSomeStrings() {
		return someStrings;
	}

	public void setSomeStrings(Set<String> someStrings) {
		this.someStrings = someStrings;
	}
}
