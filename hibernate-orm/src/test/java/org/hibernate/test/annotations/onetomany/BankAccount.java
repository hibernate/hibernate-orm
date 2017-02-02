/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

/**
 * @author Brett Meyer
 */
@Entity
public class BankAccount {

	@Id
	@GeneratedValue
	private long id;

	@OneToMany(mappedBy = "account", cascade = { CascadeType.ALL })
	@OrderColumn(name = "transactions_index")
	private List<Transaction> transactions;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public void addTransaction(String code) {
		if ( transactions == null ) {
			transactions = new ArrayList<Transaction>();
		}
		Transaction transaction = new Transaction();
		transaction.setCode( code );
		transactions.add( transaction );
	}
}
