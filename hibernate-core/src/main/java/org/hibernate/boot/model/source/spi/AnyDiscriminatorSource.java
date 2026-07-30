/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import java.util.Map;

import org.hibernate.boot.model.naming.ImplicitAnyDiscriminatorColumnNameSource;

/**
 * Source information about the discriminator for an ANY mapping
 *
 * @author Steve Ebersole
 */
@Remove
public interface AnyDiscriminatorSource extends ImplicitAnyDiscriminatorColumnNameSource {
	HibernateTypeSource getTypeSource();
	RelationalValueSource getRelationalValueSource();
	Map<String,String> getValueMappings();
}
