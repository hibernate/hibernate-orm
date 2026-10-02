package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnUniqueable extends JaxbColumn {
	Boolean isUnique();
}
