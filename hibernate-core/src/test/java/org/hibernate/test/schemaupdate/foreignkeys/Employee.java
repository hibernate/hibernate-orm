/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
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
