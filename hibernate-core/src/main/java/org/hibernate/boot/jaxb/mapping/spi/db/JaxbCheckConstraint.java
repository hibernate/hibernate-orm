package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbCheckConstraint {
	String getName();
	String getConstraint();
	String getOptions();
}
