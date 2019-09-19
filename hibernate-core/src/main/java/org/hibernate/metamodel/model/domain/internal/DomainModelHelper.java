/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import javax.persistence.metamodel.Bindable;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.Queryable;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmTreeTransformationLogger;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicType;

/**
 * Helper containing utilities useful for domain model handling

 * @author Steve Ebersole
 */
public class DomainModelHelper {

	@SuppressWarnings("unchecked")
	public static <T, S extends T> ManagedDomainType<S> resolveSubType(
			ManagedDomainType<T> baseType,
			String subTypeName,
			JpaMetamodel jpaMetamodel) {
		if ( baseType instanceof EmbeddableDomainType<?> ) {
			// todo : at least validate the string is a valid sub-type of the embeddable class?
			return (ManagedDomainType) baseType;
		}

		// first, try to find it by name directly..
		ManagedDomainType<S> subManagedType = jpaMetamodel.entity( subTypeName );
		if ( subManagedType != null ) {
			return subManagedType;
		}

		// it could still be a mapped-superclass
		try {
			final Class javaType = jpaMetamodel.getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( subTypeName );
			return jpaMetamodel.managedType( javaType );
		}
		catch (Exception ignore) {
		}

		throw new IllegalArgumentException( "Unknown sub-type name (" + baseType.getTypeName() + ") : " + subTypeName );
	}

	public static <S> ManagedDomainType<S> resolveSubType(
			ManagedDomainType<? super S> baseType,
			Class<S> subTypeClass,
			JpaMetamodel jpaMetamodel) {
		// todo : validate the hierarchy-ness...
		return jpaMetamodel.managedType( subTypeClass );
	}


}
