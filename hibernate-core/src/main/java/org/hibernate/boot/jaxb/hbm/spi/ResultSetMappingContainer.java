/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface ResultSetMappingContainer {
	List<JaxbHbmResultSetMappingType> getResultset();
}
