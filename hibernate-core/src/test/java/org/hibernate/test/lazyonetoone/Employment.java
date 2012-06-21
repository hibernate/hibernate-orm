//$Id: Employment.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.lazyonetoone;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Employment implements Serializable {
	private String personName;
	private String organizationName;
	private Date startDate;
	private Date endDate;
	Employment() {}
	public Employment(Employee e, String org) {
		this.personName = e.getPersonName();
		this.organizationName = org;
		startDate = new Date();
		e.getEmployments().add(this);
	}
	public String getOrganizationName() {
		return organizationName;
	}
	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}
	public String getPersonName() {
		return personName;
	}
	public void setPersonName(String personName) {
		this.personName = personName;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
}
