/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.revisioninfo;

import java.util.Set;

import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.tools.ReflectionTools;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.service.ServiceRegistry;

/**
 * Returns modified entity names from a persisted revision info entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ModifiedEntityNamesReader {
	private final Getter modifiedEntityNamesGetter;

	public ModifiedEntityNamesReader(
			Class<?> revisionInfoClass,
			PropertyData modifiedEntityNamesData,
			ServiceRegistry serviceRegistry) {
		modifiedEntityNamesGetter = ReflectionTools.getGetter( revisionInfoClass, modifiedEntityNamesData, serviceRegistry );
	}

	@SuppressWarnings({"unchecked"})
	public Set<String> getModifiedEntityNames(Object revisionEntity) {
		return (Set<String>) modifiedEntityNamesGetter.get( revisionEntity );
	}
}
