//$Id: Contract.java 7222 2005-06-19 17:22:01Z oneovthafew $
package org.hibernate.test.immutable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Party implements Serializable {

	private long id;
	private Contract contract;
	private String name;

	public Party() {
		super();
	}

	public Party(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}
}