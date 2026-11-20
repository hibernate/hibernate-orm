package org.hibernate;

/**
 * Thrown when {@linkplain org.hibernate.cfg.MappingSettings#TEMPORAL_PRESERVATION
 * temporal preservation} is enabled and Hibernate declines to bind, render or
 * read a {@linkplain java.time Java Time} value because it cannot guarantee
 * that the value is preserved.
 * <p>
 * This happens when the value lies outside the range for which the mapping is
 * known to preserve, or when the operation uses a combination of Java type and
 * JDBC type which the {@linkplain org.hibernate.dialect.Dialect Dialect} does
 * not report as preserving.
 *
 * @since 8.1
 *
 * @author Steve Ebersole
 */
@Incubating(since = "8.1")
public class TemporalPreservationException extends HibernateException {
	public TemporalPreservationException(String message) {
		super( message );
	}

	public TemporalPreservationException(String message, Throwable cause) {
		super( message, cause );
	}
}
