/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	public ToolingHintContext getToolingHintContext();

	public String determineEntityName(EntityInfo entityElement);

	public String determineEntityName(String entityName, String clazz);

	public String qualifyClassName(String name);

	public PersistentClass findEntityBinding(String entityName, String clazz);
}
