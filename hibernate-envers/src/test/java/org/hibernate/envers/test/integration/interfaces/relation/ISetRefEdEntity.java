package org.hibernate.envers.test.integration.interfaces.relation;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ISetRefEdEntity {
	Integer getId();

	void setId(Integer id);

	String getData();

	void setData(String data);
}
