/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A generator that {@code select}s the just-{@code insert}ed row to determine the
 * {@code IDENTITY} column value assigned by the database. The correct row is located
 * using a unique key of the entity, either:
 * <ul>
 * <li>the mapped {@linkplain org.hibernate.annotations.NaturalId} of the entity, or
 * <li>a property specified using the parameter named {@code "key"}.
 * </ul>
 * The second approach is provided for backward compatibility with older versions of
 * Hibernate.
 * <p>
 * Arguably, this class breaks the natural separation of responsibility between the
 * {@linkplain InDatabaseGenerator generator} and the coordinating
 * code, since it's role is to specify how the generated value is <em>retrieved</em>.
 *
 * @see org.hibernate.annotations.NaturalId
 *
 * @author Gavin King
 */
public class SelectGenerator extends IdentityGenerator {
	private String uniqueKeyPropertyName;

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) {
		uniqueKeyPropertyName = parameters.getProperty( "key" );
	}

	@Override
	public String getUniqueKeyPropertyName(EntityPersister persister) {
		if ( uniqueKeyPropertyName != null ) {
			return uniqueKeyPropertyName;
		}
		int[] naturalIdPropertyIndices = persister.getNaturalIdentifierProperties();
		if ( naturalIdPropertyIndices == null ) {
			throw new IdentifierGenerationException(
					"no natural-id property defined; need to specify [key] in " +
							"generator parameters"
			);
		}
		if ( naturalIdPropertyIndices.length > 1 ) {
			throw new IdentifierGenerationException(
					"select generator does not currently support composite " +
							"natural-id properties; need to specify [key] in generator parameters"
			);
		}
		if ( persister.getEntityMetamodel().isNaturalIdentifierInsertGenerated() ) {
			throw new IdentifierGenerationException(
					"natural-id also defined as insert-generated; need to specify [key] " +
							"in generator parameters"
			);
		}
		return persister.getPropertyNames()[naturalIdPropertyIndices[0]];
	}
}
