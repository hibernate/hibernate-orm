/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;


/**
 * @author Brett Meyer
 */
@Entity
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
public class Employee {
	@Id @GeneratedValue
	public long id;
	
	@ManyToMany
	public Set<Manager> managers = new HashSet<Manager>();
	
	@ManyToMany
	public Set<Employee> friends = new HashSet<Employee>();
}
