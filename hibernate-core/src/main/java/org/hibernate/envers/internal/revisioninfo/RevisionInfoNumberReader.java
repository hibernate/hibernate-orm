/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.service.ServiceRegistry;

/**
 * Gets a revision number from a persisted revision info entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionInfoNumberReader {
	private final Getter revisionIdGetter;

	public RevisionInfoNumberReader(Class<?> revisionInfoClass, PropertyData revisionInfoIdData, ServiceRegistry serviceRegistry) {
		revisionIdGetter = ReflectionTools.getGetter( revisionInfoClass, revisionInfoIdData, serviceRegistry );
	}

	public Number getRevisionNumber(Object revision) {
		return (Number) revisionIdGetter.get( revision );
	}
}
