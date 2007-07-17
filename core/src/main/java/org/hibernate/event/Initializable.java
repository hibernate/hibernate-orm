//$Id: Initializable.java 7793 2005-08-10 05:06:40Z oneovthafew $
package org.hibernate.event;

import org.hibernate.cfg.Configuration;

/**
 * An event listener that requires access to mappings to
 * initialize state at initialization time.
 * @author Gavin King
 */
public interface Initializable {
	public void initialize(Configuration cfg);
}
