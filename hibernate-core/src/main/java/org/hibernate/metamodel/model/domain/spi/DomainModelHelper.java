/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Helper containing utilities useful for domain model handling

 * @author Steve Ebersole
 */
public class DomainModelHelper {
	public static EntityPersister resolveEntityPersister(
			EntityTypeDescriptor<?> entityType,
			SessionFactoryImplementor sessionFactory) {
		// Our EntityTypeImpl#getType impl returns the Hibernate entity-name
		// which is exactly what we want
		final String hibernateEntityName = entityType.getName();
		return sessionFactory.getMetamodel().entityPersister( hibernateEntityName );
	}

	@SuppressWarnings("unchecked")
	public static <T, S extends T> ManagedTypeDescriptor<S> resolveSubType(
			ManagedTypeDescriptor<T> baseType,
			String subTypeName,
			SessionFactoryImplementor sessionFactory) {
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();

		if ( baseType instanceof EmbeddedTypeDescriptor<?> ) {
			// todo : at least validate the string is a valid sub-type of the embeddable class?
			return (ManagedTypeDescriptor) baseType;
		}

		final String importedClassName = metamodel.getImportedClassName( subTypeName );
		if ( importedClassName != null ) {
			// first, try to find it by name directly..
			ManagedTypeDescriptor<S> subManagedType = metamodel.entity( importedClassName );
			if ( subManagedType != null ) {
				return subManagedType;
			}

			// it could still be a mapped-superclass
			try {
				final Class<S> subTypeClass = sessionFactory.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.classForName( importedClassName );

				return metamodel.managedType( subTypeClass );
			}
			catch (Exception ignore) {
			}
		}

		throw new IllegalArgumentException( "Unknown sub-type name (" + baseType.getName() + ") : " + subTypeName );
	}

	public static <S> ManagedTypeDescriptor<S> resolveSubType(
			ManagedTypeDescriptor<? super S> baseType,
			Class<S> subTypeClass,
			SessionFactoryImplementor sessionFactory) {
		// todo : validate the hierarchy-ness...
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		return metamodel.managedType( subTypeClass );
	}
}
