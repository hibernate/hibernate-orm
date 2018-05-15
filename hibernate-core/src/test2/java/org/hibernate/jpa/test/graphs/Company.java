/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;


/**
 * @author Brett Meyer
 */
@Entity
public class Company {
	@Id @GeneratedValue
	public long id;
	
	@OneToMany
	public Set<Employee> employees = new HashSet<Employee>();
	
	@OneToOne(fetch = FetchType.LAZY)
	public Location location;
	
	@ElementCollection
	public Set<Market> markets = new HashSet<Market>();
	
	@ElementCollection(fetch = FetchType.EAGER)
	public Set<String> phoneNumbers = new HashSet<String>();

	public Location getLocation() {
		return location;
	}
}
