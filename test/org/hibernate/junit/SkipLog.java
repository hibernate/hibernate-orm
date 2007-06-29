package org.hibernate.junit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Well-known-location lookup for the test-skip log...
 *
 * @author Steve Ebersole
 */
public class SkipLog {
	public static final Log LOG = LogFactory.getLog( "org.hibernate.test.SKIPPED" );
}
