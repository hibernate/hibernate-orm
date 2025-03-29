/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ContractVariation implements Serializable {

	private int version;
	private Contract contract;
	private String text;
	private Set infos = new HashSet();

	public Contract getContract() {
		return contract;
	}

	public void setContract(Contract contract) {
		this.contract = contract;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public ContractVariation() {
		super();
	}

	public ContractVariation(int version, Contract contract) {
		this.contract = contract;
		this.version = version;
		contract.getVariations().add(this);
	}

	public Set getInfos() {
		return infos;
	}

	public void setInfos(Set infos) {
		this.infos = infos;
	}
}
