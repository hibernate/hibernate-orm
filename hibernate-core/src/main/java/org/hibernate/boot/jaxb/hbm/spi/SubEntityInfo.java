/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

/**
 * Common interface for all sub-entity mappings.
 *
 * @author Steve Ebersole
 */
public interface SubEntityInfo extends EntityInfo {
	String getExtends();
}
