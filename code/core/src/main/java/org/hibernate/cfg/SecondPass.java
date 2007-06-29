//$Id: SecondPass.java 10196 2006-08-03 07:53:27Z max.andersen@jboss.com $
package org.hibernate.cfg;

import java.io.Serializable;

import org.hibernate.MappingException;

/**
 * Second pass operation
 *
 * @author Emmanuel Bernard
 */
public interface SecondPass extends Serializable {

	void doSecondPass(java.util.Map persistentClasses)
				throws MappingException;

}
