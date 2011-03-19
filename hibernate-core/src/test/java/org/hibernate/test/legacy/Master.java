//$Id: Master.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Master implements Serializable, Named {
	private Long id;
	private Master otherMaster;
	private Set details = new HashSet();
	private Set moreDetails = new HashSet();
	private Set incoming = new HashSet();
	private Set outgoing = new HashSet();
	private String name="master";
	private Date stamp;
	private int version;
	private BigDecimal bigDecimal = new BigDecimal("1234.123");
	private int x;
	private Collection allDetails;

	public Master() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}
	
	public Set getDetails() {
		return details;
	}
	
	private void setDetails(Set details) {
		this.details = details;
	}
	
	public void addDetail(Detail d) {
		details.add(d);
	}
	
	public void removeDetail(Detail d) {
		details.remove(d);
	}
	
	public void addIncoming(Master m) {
		incoming.add(m);
	}
	
	public void removeIncoming(Master m) {
		incoming.remove(m);
	}
	
	public void addOutgoing(Master m) {
		outgoing.add(m);
	}
	
	public void removeOutgoing(Master m) {
		outgoing.remove(m);
	}
	
	public Set getIncoming() {
		return incoming;
	}
	
	public void setIncoming(Set incoming) {
		this.incoming = incoming;
	}
	
	public Set getOutgoing() {
		return outgoing;
	}
	
	public void setOutgoing(Set outgoing) {
		this.outgoing = outgoing;
	}
	
	public Set getMoreDetails() {
		return moreDetails;
	}
	
	public void setMoreDetails(Set moreDetails) {
		this.moreDetails = moreDetails;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Date getStamp() {
		return stamp;
	}
	
	public void setStamp(Date stamp) {
		this.stamp = stamp;
	}
	
	public BigDecimal getBigDecimal() {
		return bigDecimal;
	}
	
	public void setBigDecimal(BigDecimal bigDecimal) {
		this.bigDecimal = bigDecimal;
	}
	
	/**
	 * @return
	 */
	public Master getOtherMaster() {
		return otherMaster;
	}

	/**
	 * @param master
	 */
	public void setOtherMaster(Master master) {
		otherMaster = master;
	}

	/**
	 * @return Returns the allDetails.
	 */
	public Collection getAllDetails() {
		return allDetails;
	}
	/**
	 * @param allDetails The allDetails to set.
	 */
	public void setAllDetails(Collection allDetails) {
		this.allDetails = allDetails;
	}
}






