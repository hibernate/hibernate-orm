/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ContractVariation implements Serializable {
	
	private int id;
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ContractVariation() {
		super();
	}

	public ContractVariation(int version, Contract contract) {
		this.contract = contract;
		contract.getVariations().add(this);
	}

	public Set getInfos() {
		return infos;
	}

	public void setInfos(Set infos) {
		this.infos = infos;
	}
}
