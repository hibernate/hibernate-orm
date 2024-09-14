/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.lazytoone.collectioninitializer;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "users")
public class User {
	@Id
	private Long id;

	@OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true)
	private List<UserAuthorization> authorizations = new ArrayList<>();

	@Override
	public String toString() {
		return "User{" +
				"id='" + getId() + '\'' +
				'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<UserAuthorization> getAuthorizations() {
		return authorizations;
	}

	public void setAuthorizations(List<UserAuthorization> authorizations) {
		this.authorizations = authorizations;
	}
}
