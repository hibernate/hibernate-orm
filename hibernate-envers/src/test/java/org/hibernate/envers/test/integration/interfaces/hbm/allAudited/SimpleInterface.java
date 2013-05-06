package org.hibernate.envers.test.integration.interfaces.hbm.allAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
@Audited
public interface SimpleInterface {

	long getId();

	void setId(long id);

	String getData();

	void setData(String data);

}
