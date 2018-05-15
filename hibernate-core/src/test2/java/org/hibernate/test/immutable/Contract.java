/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Contract.java 7222 2005-06-19 17:22:01Z oneovthafew $
package org.hibernate.test.immutable;
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
	private Set plans;
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
