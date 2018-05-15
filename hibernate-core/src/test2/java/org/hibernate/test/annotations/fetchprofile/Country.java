/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.fetchprofile;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="Order_Country")
public class Country {
	@Id @GeneratedValue
	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	private Integer id;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;
}
