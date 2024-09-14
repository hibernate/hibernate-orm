/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.referencedcolumnname;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Postman implements Serializable {
	private String name;
	private String id;

	public Postman() {
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Postman(String name, String id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
