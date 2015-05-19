/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Employee {
	@Id
	long empId;
	String empName;

	@OneToMany(mappedBy = "emp", fetch = FetchType.LAZY)
	private Set<Dependent> dependents;
}
