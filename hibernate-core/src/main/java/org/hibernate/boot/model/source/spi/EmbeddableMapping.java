/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;

/**
 * Unifying contract for consuming JAXB types which describe an embeddable (in JPA terms).
 *
 * @author Steve Ebersole
 */
@Remove
public interface EmbeddableMapping {
	String getClazz();
	List<JaxbHbmTuplizerType> getTuplizer();

	String getParent();
}
