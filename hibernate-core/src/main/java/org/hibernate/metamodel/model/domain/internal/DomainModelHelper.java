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
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.spi.DomainMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
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


	/**
	 * Resolve a JPA EntityType descriptor to it's corresponding EntityPersister
	 * in the Hibernate mapping type system
	 */
	public static EntityPersister resolveEntityPersister(
			EntityDomainType<?> entityType,
			SessionFactoryImplementor sessionFactory) {
		// Our EntityTypeImpl#getType impl returns the Hibernate entity-name
		// which is exactly what we want
		final String hibernateEntityName = entityType.getHibernateEntityName();
		return sessionFactory.getMetamodel().entityPersister( hibernateEntityName );
	}

	public static <J> SqmPathSource<J> resolveSqmPathSource(
			String name,
			DomainType<J> valueDomainType,
			Bindable.BindableType jpaBindableType) {

		if ( valueDomainType instanceof BasicDomainType ) {
			return new BasicSqmPathSource<>(
					name,
					(BasicDomainType<J>) valueDomainType,
					jpaBindableType
			);
		}

		if ( valueDomainType instanceof AnyMappingDomainType ) {
			return new AnyMappingSqmPathSource<>(
					name,
					(AnyMappingDomainType<J>) valueDomainType,
					jpaBindableType
			);
		}

		if ( valueDomainType instanceof EmbeddableDomainType ) {
			return new EmbeddedSqmPathSource<>(
					name,
					(EmbeddableDomainType<J>) valueDomainType,
					jpaBindableType
			);
		}

		if ( valueDomainType instanceof EntityDomainType ) {
			return new EntitySqmPathSource<>(
					name,
					(EntityDomainType<J>) valueDomainType,
					jpaBindableType
			);
		}

		throw new IllegalArgumentException(
				"Unrecognized value type Java-type [" + valueDomainType.getTypeName() + "] for plural attribute value"
		);
	}

	public static MappingModelExpressable resolveMappingModelExpressable(
			SqmTypedNode<?> sqmNode,
			SqlAstCreationState creationState) {
		if ( sqmNode instanceof SqmPath ) {
			return resolveSqmPath( (SqmPath) sqmNode, creationState );
		}

		final SqmExpressable<?> nodeType = sqmNode.getNodeType();
		if ( nodeType instanceof BasicType ) {
			return ( (BasicType) nodeType );
		}

		throw new NotYetImplementedFor6Exception( DomainModelHelper.class );
	}

	private static ModelPart resolveSqmPath(SqmPath<?> sqmPath, SqlAstCreationState creationState) {
		final DomainMetamodel domainModel = creationState.getCreationContext().getDomainModel();

		if ( sqmPath instanceof SqmTreatedPath ) {
			final SqmTreatedPath treatedPath = (SqmTreatedPath) sqmPath;
			final EntityDomainType treatTargetType = treatedPath.getTreatTarget();
			return domainModel.findEntityDescriptor( treatTargetType.getHibernateEntityName() );
		}

		// see if the LHS is treated
		if ( sqmPath.getLhs() instanceof SqmTreatedPath ) {
			final SqmTreatedPath treatedPath = (SqmTreatedPath) sqmPath.getLhs();
			final EntityDomainType treatTargetType = treatedPath.getTreatTarget();
			final EntityPersister container = domainModel.findEntityDescriptor( treatTargetType.getHibernateEntityName() );

			return container.findSubPart( sqmPath.getNavigablePath().getLocalName(), container );
		}

		final ModelPartContainer container = resolveLhs( sqmPath, creationState );
		return container.findSubPart( sqmPath.getNavigablePath().getLocalName(), null );
	}

	private static ModelPartContainer resolveLhs(SqmPath<?> sqmPath, SqlAstCreationState creationState) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		final SqmPathSource<?> referencedPathSource = lhs.getReferencedPathSource();

		if ( referencedPathSource instanceof EntityDomainType ) {
			return resolveEntityPersister(  (EntityDomainType) referencedPathSource, creationState.getCreationContext().getSessionFactory() );
		}
		else if ( referencedPathSource instanceof SingularPersistentAttribute ) {
			final SingularPersistentAttribute attribute = (SingularPersistentAttribute) referencedPathSource;
			if ( attribute.getType() instanceof EntityDomainType ) {
				return resolveEntityPersister(  (EntityDomainType) attribute.getType(), creationState.getCreationContext().getSessionFactory() );
			}

			throw new NotYetImplementedFor6Exception( "Support for composite sub-paths not yet implemented" );
		}

		throw new NotYetImplementedFor6Exception( );
	}
}
