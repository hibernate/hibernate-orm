/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Plan implements Serializable {

	private long id;
	private long version;
	private String description;
	private Set contracts;
	private Set infos;

	public Plan() {
		this( null );
	}

	public Plan(String description) {
		this.description = description;
		contracts = new HashSet();
		infos = new HashSet();
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set getContracts() {
		return contracts;
	}

	public void setContracts(Set contracts) {
		this.contracts = contracts;
	}

	public void addContract(Contract contract) {
		if ( ! contracts.add( contract ) ) {
			return;
		}
		if ( contract.getParent() != null ) {
			addContract( contract.getParent() );
		}
		contract.getPlans().add( this );
		for ( Iterator it=contract.getSubcontracts().iterator(); it.hasNext(); ) {
			Contract sub = ( Contract ) it.next();
			addContract( sub );
		}
	}

	public void removeContract(Contract contract) {
		if ( contract.getParent() != null ) {
			contract.getParent().getSubcontracts().remove( contract );
			contract.setParent( null );
		}
		removeSubcontracts( contract );
		contract.getPlans().remove( this );
		contracts.remove( contract );
	}

	public void removeSubcontracts(Contract contract) {
		for ( Iterator it=contract.getSubcontracts().iterator(); it.hasNext(); ) {
			Contract sub = ( Contract ) it.next();
			removeSubcontracts( sub );
			sub.getPlans().remove( this );
			contracts.remove( sub );
		}
	}

	public Set getInfos() {
		return infos;
	}

	public void setInfos(Set infos) {
		this.infos = infos;
	}
}
