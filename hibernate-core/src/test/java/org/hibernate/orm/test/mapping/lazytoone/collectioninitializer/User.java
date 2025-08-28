/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
