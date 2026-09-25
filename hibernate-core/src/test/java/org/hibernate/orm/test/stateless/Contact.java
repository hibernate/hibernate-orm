package org.hibernate.orm.test.stateless;


/**
 *
 * @author stliu
 */
public class Contact {
	private Integer id;
	public Integer getId() {
		return id;
	}
	public void setId( Integer id ) {
		this.id = id;
	}
	public Org getOrg() {
		return org;
	}
	public void setOrg( Org org ) {
		this.org = org;
	}
	private Org org;

}
