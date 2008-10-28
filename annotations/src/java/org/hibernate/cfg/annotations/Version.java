//$Id$
package org.hibernate.cfg.annotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class Version {
	public static final String VERSION = "3.4.0.GA";
	private static Logger log = LoggerFactory.getLogger( Version.class );

	static {
		log.info( "Hibernate Annotations {}", VERSION );
	}

	public static void touch() {
	}
}
