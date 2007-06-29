//$Id$
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
