/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.internal;

import java.util.Map;

import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.sql.ast.produce.result.spi.EntityReference;
import org.hibernate.sql.ast.consume.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class EntityReturnInitializerImpl
		extends AbstractEntityReferenceInitializer
		implements EntityReferenceInitializer {
	public EntityReturnInitializerImpl(
			EntityReference entityReference,
			Map<PersistentAttribute,SqlSelectionGroup> sqlSelectionGroupMap,
			boolean isShallow) {
		super( null, entityReference, true, sqlSelectionGroupMap, isShallow );
	}
}
