/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * A model part that is (or can be) embeddable-valued (composite) - {@linkplain JaxbEmbeddedIdImpl},
 * {@linkplain JaxbEmbeddedIdImpl} and {@linkplain JaxbElementCollectionImpl}
 *
 * @author Steve Ebersole
 */
public interface JaxbEmbeddedMapping extends JaxbSingularAttribute {
}
