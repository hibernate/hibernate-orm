package org.hibernate.orm.test.envers.integration.interfaces.components;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface IComponent {
	String getData();

	void setData(String data);
}
