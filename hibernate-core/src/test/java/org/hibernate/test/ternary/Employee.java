//$Id: Employee.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.ternary;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Employee {
	private String name;
	private Date hireDate;
	private Map managerBySite = new HashMap();
	private Set underlings = new HashSet();
	
	Employee() {}
	public Employee(String name) {
		this.name=name;
	}
	public Map getManagerBySite() {
		return managerBySite;
	}
	public void setManagerBySite(Map managerBySite) {
		this.managerBySite = managerBySite;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set getUnderlings() {
		return underlings;
	}
	public void setUnderlings(Set underlings) {
		this.underlings = underlings;
	}
	public Date getHireDate() {
		return hireDate;
	}
	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}
}
