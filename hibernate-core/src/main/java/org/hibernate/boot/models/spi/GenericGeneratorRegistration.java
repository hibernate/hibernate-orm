/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.spi;

import java.util.Map;

/**
 * Global registration of a generic generator
 *
 * @author Steve Ebersole
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl
 */
public record GenericGeneratorRegistration(String name, String strategy, Map<String, String> parameters) {
}
