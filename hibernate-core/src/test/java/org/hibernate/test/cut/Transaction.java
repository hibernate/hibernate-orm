//$Id: Transaction.java 6234 2005-03-29 03:07:30Z oneovthafew $
package org.hibernate.test.cut;


/**
 * @author Gavin King
 */
public class Transaction {

	private Long id;
	private String description;
	private MonetoryAmount value;
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public MonetoryAmount getValue() {
		return value;
	}
	
	public void setValue(MonetoryAmount value) {
		this.value = value;
	}

}
