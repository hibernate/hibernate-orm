/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import org.hibernate.metamodel.mapping.PluralAttributeMapping;

/**
 * @author Steve Ebersole
 */
public interface ModelPartResultMementoCollection extends ModelPartResultMemento {
	PluralAttributeMapping getPluralAttributeDescriptor();
}
