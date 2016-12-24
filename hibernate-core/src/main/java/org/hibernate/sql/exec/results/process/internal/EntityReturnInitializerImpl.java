/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.util.Map;

import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.sql.convert.results.spi.EntityReference;
import org.hibernate.sql.exec.results.process.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class EntityReturnInitializerImpl
		extends AbstractEntityReferenceInitializer
		implements EntityReferenceInitializer {
	public EntityReturnInitializerImpl(
			EntityReference entityReference,
			Map<Attribute,SqlSelectionGroup> sqlSelectionGroupMap,
			boolean isShallow) {
		super( null, entityReference, true, sqlSelectionGroupMap, isShallow );
	}
}
