/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class NaturalIdHelper {
	public static String[] getNaturalIdPropertyNames(EntityPersister persister) {
		final int[] naturalIdPropertyIndices = persister.getNaturalIdentifierProperties();
		if ( naturalIdPropertyIndices == null ) {
			throw new IdentifierGenerationException( "entity '" + persister.getEntityName()
					+ "' has no '@NaturalId' property" );
		}
		if ( persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			throw new IdentifierGenerationException( "entity '" + persister.getEntityName()
					+ "' has a '@NaturalId' property which is also defined as insert-generated" );
		}
		final String[] allPropertyNames = persister.getPropertyNames();
		final String[] propertyNames = new String[naturalIdPropertyIndices.length];
		for ( int i = 0; i < naturalIdPropertyIndices.length; i++ ) {
			propertyNames[i] = allPropertyNames[naturalIdPropertyIndices[i]];
		}
		return propertyNames;
	}
}
