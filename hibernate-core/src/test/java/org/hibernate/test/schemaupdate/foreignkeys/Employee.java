/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "EMPLOYEE")
public class Employee {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToMany
	@JoinTable(name = "EMPLOYEE_PROJECT",
			joinColumns = @JoinColumn(name = "EMPLOYEE_ID", foreignKey = @ForeignKey(name = "FK_EMPLOYEE")),
			inverseJoinColumns = @JoinColumn(name = "PROJECT_ID", foreignKey = @ForeignKey(name = "FK_PROJECT")))
	private Set<Project> projects;

}
