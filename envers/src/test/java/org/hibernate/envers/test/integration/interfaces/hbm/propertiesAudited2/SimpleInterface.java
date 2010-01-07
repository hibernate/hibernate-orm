package org.hibernate.envers.test.integration.interfaces.hbm.propertiesAudited2;

import org.hibernate.envers.Audited;

/**
 * @author Hernán Chanfreau
 *
 */

public interface SimpleInterface {
	
	long getId();
	
	void setId(long id);
	
	@Audited
	String getData();
	
	void setData(String data);
	
	@Audited
	int getNumerito();
	
	void setNumerito(int num);

}
