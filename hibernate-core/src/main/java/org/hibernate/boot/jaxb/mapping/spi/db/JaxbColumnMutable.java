package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnMutable extends JaxbColumn {
	Boolean isInsertable();
	Boolean isUpdatable();
}
