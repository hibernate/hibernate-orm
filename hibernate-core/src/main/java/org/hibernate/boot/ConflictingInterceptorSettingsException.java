package org.hibernate.boot;

import org.hibernate.HibernateException;
import org.hibernate.cfg.SessionEventSettings;

/**
 * Indicates that both {@value SessionEventSettings#INTERCEPTOR} and
 * {@value SessionEventSettings#SESSION_SCOPED_INTERCEPTOR} were configured.
 * At most one of these settings may be specified at a time.
 */
public class ConflictingInterceptorSettingsException extends HibernateException {
	public ConflictingInterceptorSettingsException() {
		super(
				"Only one of '" + SessionEventSettings.INTERCEPTOR + "' and '"
						+ SessionEventSettings.SESSION_SCOPED_INTERCEPTOR + "' may be configured, not both"
		);
	}
}
