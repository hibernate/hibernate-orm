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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Contract implements Serializable {
	
	private long id;
	private long version;
	private String customerName;
	private String type;
	private List variations;
	private Contract parent;
	private Set subcontracts;
	private Set plans = new HashSet();
	private Set parties;
	private Set infos;

	public Contract() {
		super();
	}

	public Contract(Plan plan, String customerName, String type) {
		plans = new HashSet();
		if ( plan != null ) {
			plans.add( plan );
			plan.getContracts().add( this );
		}
		this.customerName = customerName;
		this.type = type;
		variations = new ArrayList();
		subcontracts = new HashSet();
		parties = new HashSet();
		infos = new HashSet();
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
	
	public Set getPlans() {
		return plans;
	}

	public void setPlans(Set plans) {
		this.plans = plans;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List getVariations() {
		return variations;
	}

	public void setVariations(List variations) {
		this.variations = variations;
	}

	public Contract getParent() {
		return parent;
	}

	public void setParent(Contract parent) {
		this.parent = parent;
	}

	public Set getSubcontracts() {
		return subcontracts;
	}

	public void setSubcontracts(Set subcontracts) {
		this.subcontracts = subcontracts;
	}

	public void addSubcontract(Contract subcontract) {
		subcontracts.add( subcontract );
		subcontract.setParent( this );
	}

	public Set getParties() {
		return parties;
	}

	public void setParties(Set parties) {
		this.parties = parties;
	}

	public void addParty(Party party) {
		parties.add( party );
		party.setContract( this );
	}

	public void removeParty(Party party) {
		parties.remove( party );
		party.setContract( null );
	}

	public Set getInfos() {
		return infos;
	}

	public void setInfos(Set infos) {
		this.infos = infos;
	}
}
