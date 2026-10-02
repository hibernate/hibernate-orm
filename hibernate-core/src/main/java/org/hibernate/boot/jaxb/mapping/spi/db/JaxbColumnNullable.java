package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnNullable extends JaxbColumn {

	Boolean isNullable();
}
