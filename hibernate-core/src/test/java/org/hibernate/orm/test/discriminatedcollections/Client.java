/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.discriminatedcollections;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
class Client {
	@Id
	private Integer id;
	private String name;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
	private Set<DebitAccount> debitAccounts = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
	private Set<CreditAccount> creditAccounts = new HashSet<>();

	Client() {}

	public Client(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}


	public Set<CreditAccount> getCreditAccounts() {
		return creditAccounts;
	}

	public Set<DebitAccount> getDebitAccounts() {
		return debitAccounts;
	}
}
