package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnDefaultable extends JaxbColumn {
	String getDefault();
}
