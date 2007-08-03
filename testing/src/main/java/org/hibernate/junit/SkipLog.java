package org.hibernate.junit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Well-known-location lookup for the test-skip log...
 *
 * @author Steve Ebersole
 */
public class SkipLog {
	public static final Logger LOG = LoggerFactory.getLogger( "org.hibernate.test.SKIPPED" );
}
