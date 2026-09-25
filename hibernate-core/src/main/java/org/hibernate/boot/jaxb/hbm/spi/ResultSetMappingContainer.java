package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface ResultSetMappingContainer {
	List<JaxbHbmResultSetMappingType> getResultset();
}
