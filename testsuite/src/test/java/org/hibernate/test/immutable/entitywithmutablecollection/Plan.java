/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.immutable.entitywithmutablecollection;

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
	private Owner owner;

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

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}