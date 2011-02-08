package org.hibernate.envers.strategy;
import org.hibernate.envers.EnversLogger;
import org.jboss.logging.Logger;

/**
 * Deprecated Audit strategy class.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 *
 * @deprecated use {@link ValidityAuditStrategy} instead.
 */
@Deprecated
public class ValidTimeAuditStrategy extends ValidityAuditStrategy {

    public static final EnversLogger LOG = Logger.getMessageLogger(EnversLogger.class, ValidTimeAuditStrategy.class.getName());

	/**
	 * Default constructor. Log a warn message that this class is deprecated.
	 */
	public ValidTimeAuditStrategy() {
        LOG.validTimeAuditStrategyDeprecated();
	}

}
