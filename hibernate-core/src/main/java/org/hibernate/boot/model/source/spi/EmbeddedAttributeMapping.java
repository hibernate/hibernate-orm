/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.jaxb.hbm.spi.SingularAttributeInfo;

/**
 * Unifying contract for any JAXB types which describe an embedded (in JPA terms).
 * <p>
 * Essentially this presents a unified contract over the {@code <component/>},
 * {@code <composite-id/>}, {@code <dynamic-component/>} and
 * {@code <nested-dynamic-component/>} elements
 *
 * @author Steve Ebersole
 */
public interface EmbeddedAttributeMapping extends SingularAttributeInfo {
	boolean isUnique();
	EmbeddableMapping getEmbeddableMapping();
}
