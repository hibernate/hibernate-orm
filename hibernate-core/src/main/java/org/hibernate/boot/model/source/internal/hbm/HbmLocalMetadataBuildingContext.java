/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.mapping.PersistentClass;

/**
 * Access to contextual information specific to a {@code hbm.xml} mapping.
 *
 * @author Steve Ebersole
 */
public interface HbmLocalMetadataBuildingContext extends LocalMetadataBuildingContext {
	ToolingHintContext getToolingHintContext();

	String determineEntityName(EntityInfo entityElement);

	String determineEntityName(String entityName, String clazz);

	String qualifyClassName(String name);

	PersistentClass findEntityBinding(String entityName, String clazz);
}
