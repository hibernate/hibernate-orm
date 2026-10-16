package org.hibernate.processor.internal;

import org.hibernate.processor.spi.HibernateProcessorExtension;

/**
 * The {@link HibernateProcessorExtension} used when no other implementation is
 * registered with {@link java.util.ServiceLoader}. It does nothing: the processor
 * behaves exactly as it would without any framework-specific support.
 */
public class DefaultHibernateProcessorExtension implements HibernateProcessorExtension {
}
