/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.generator.internal;

import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.persister.entity.EntityPersister;

public class NaturalIdHelper {
	public static String getNaturalIdPropertyName(EntityPersister persister) {
		int[] naturalIdPropertyIndices = persister.getNaturalIdentifierProperties();
		if ( naturalIdPropertyIndices == null ) {
			throw new IdentifierGenerationException(
					"no natural-id property defined; " +
							"need to specify [key] in generator parameters"
			);
		}
		if ( naturalIdPropertyIndices.length > 1 ) {
			throw new IdentifierGenerationException(
					"generator does not currently support composite natural-id properties;" +
							" need to specify [key] in generator parameters"
			);
		}
		if ( persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			throw new IdentifierGenerationException(
					"natural-id also defined as insert-generated; " +
							"need to specify [key] in generator parameters"
			);
		}
		return persister.getPropertyNames()[naturalIdPropertyIndices[0]];
	}
}
