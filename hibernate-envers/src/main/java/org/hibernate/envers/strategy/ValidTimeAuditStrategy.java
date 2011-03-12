package org.hibernate.envers.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deprecated Audit strategy class.
 * 
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * 
 * @deprecated use {@link ValidityAuditStrategy} instead.
 */
public class ValidTimeAuditStrategy extends ValidityAuditStrategy {

	private final Logger log = LoggerFactory.getLogger(ValidTimeAuditStrategy.class);

	/**
	 * Default constructor. Log a warn message that this class is deprecated.
	 */
	public ValidTimeAuditStrategy() {
		log.warn("ValidTimeAuditStrategy is deprecated, please use ValidityAuditStrategy instead");
	}

}
