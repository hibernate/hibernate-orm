//$Id: Contract.java 7222 2005-06-19 17:22:01Z oneovthafew $
package org.hibernate.test.immutable;

import java.io.Serializable;

public class Info implements Serializable {

	private long id;
	private String text;
	private Contract contract;

	public Info() {
		super();
	}

	public Info(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
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

	public void setContract(Contract contract ) {
		this.contract = contract;
	}
}