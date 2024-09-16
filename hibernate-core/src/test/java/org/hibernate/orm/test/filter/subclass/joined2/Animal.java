/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.subclass.joined2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import static jakarta.persistence.InheritanceType.JOINED;

@Entity
@Table(name = "animals")
@Inheritance(strategy = JOINED)
@Filter(name = "companyFilter", condition = "id_company = :companyIdParam")
public class Animal {
	@Id
	@Column(name = "id_animal")
	private int id;

	private String name;

	@Column(name = "id_company")
	private long company;
}
