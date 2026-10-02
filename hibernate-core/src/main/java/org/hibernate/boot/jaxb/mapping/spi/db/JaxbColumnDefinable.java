package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnDefinable extends JaxbColumn {
	String getColumnDefinition();
	String getOptions();
}
