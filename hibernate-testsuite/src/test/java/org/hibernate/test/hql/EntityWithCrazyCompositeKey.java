// $Id: EntityWithCrazyCompositeKey.java 6567 2005-04-27 17:41:57Z steveebersole $
package org.hibernate.test.hql;

/**
 * Implementation of EntityWithCrazyCompositeKey.
 *
 * @author Steve Ebersole
 */
public class EntityWithCrazyCompositeKey {
	private CrazyCompositeKey id;
	private String name;

	public CrazyCompositeKey getId() {
		return id;
	}

	public void setId(CrazyCompositeKey id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
