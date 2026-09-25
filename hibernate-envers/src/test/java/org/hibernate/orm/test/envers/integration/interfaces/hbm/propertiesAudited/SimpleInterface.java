package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
public interface SimpleInterface {

	long getId();

	void setId(long id);

	String getData();

	void setData(String data);

	@Audited
	int getNumerito();

	void setNumerito(int num);

}
