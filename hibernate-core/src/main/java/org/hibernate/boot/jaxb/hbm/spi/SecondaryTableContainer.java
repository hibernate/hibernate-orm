/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Common interface for mappings that may contain secondary table (join) mappings.
 *
 * @author Steve Ebersole
 */
public interface SecondaryTableContainer {
	List<JaxbHbmSecondaryTableType> getJoin();
}
