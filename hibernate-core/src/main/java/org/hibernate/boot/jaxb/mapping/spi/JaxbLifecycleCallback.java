package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for lifecycle callbacks.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbLifecycleCallback {
	String getMethodName();
}
