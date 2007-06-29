//$Id: Customer.java 5011 2004-12-19 22:01:25Z maxcsaucdk $
package org.hibernate.test.extendshbm;

/**
 * @author Gavin King
 */
public class Customer extends Person {
	private Employee salesperson;
	private String comments;

	/**
	 * @return Returns the salesperson.
	 */
	public Employee getSalesperson() {
		return salesperson;
	}
	/**
	 * @param salesperson The salesperson to set.
	 */
	public void setSalesperson(Employee salesperson) {
		this.salesperson = salesperson;
	}
	/**
	 * @return Returns the comments.
	 */
	public String getComments() {
		return comments;
	}
	/**
	 * @param comments The comments to set.
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}
}
