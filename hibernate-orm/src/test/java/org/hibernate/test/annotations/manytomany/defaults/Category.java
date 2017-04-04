/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.manytomany.defaults;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * @author Gail Badner
 */
@Entity(name="CATEGORY")
@Table(name="CATEGORY_TAB")
public class Category {
	private Integer id;
	private Set<KnownClient> clients;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToMany
	public Set<KnownClient> getClients() {
		return clients;
	}

	public void setClients(Set<KnownClient> clients) {
		this.clients = clients;
	}
}
