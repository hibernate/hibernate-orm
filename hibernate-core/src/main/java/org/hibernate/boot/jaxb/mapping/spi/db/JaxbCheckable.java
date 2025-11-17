/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;

/**
 * @author Steve Ebersole
 */
public interface JaxbCheckable {

	List<JaxbCheckConstraintImpl> getCheckConstraints();
}
