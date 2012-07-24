/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.metamodel.internal.builder;

import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.metamodel.internal.AbstractIdentifiableType;
import org.hibernate.jpa.metamodel.internal.AbstractManagedType;
import org.hibernate.jpa.metamodel.internal.BasicTypeImpl;
import org.hibernate.jpa.metamodel.internal.EmbeddableTypeImpl;
import org.hibernate.jpa.metamodel.internal.MappedSuperclassTypeImpl;
import org.hibernate.jpa.metamodel.internal.PluralAttributeImpl;
import org.hibernate.jpa.metamodel.internal.SingularAttributeImpl;
import org.hibernate.jpa.metamodel.internal.UnsupportedFeature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.domain.PluralAttributeNature;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;

import static javax.persistence.metamodel.Attribute.PersistentAttributeType;

/**
 * A factory for building {@link Attribute} instances.  Exposes 3 main services:<ol>
 * <li>{@link #buildAttribute} for building normal attributes</li>
 * <li>{@link #buildIdAttribute} for building identifier attributes</li>
 * <li>{@link #buildVersionAttribute} for building version attributes}</li>
 * <ol>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class AttributeBuilder {
    private static final Logger LOG = Logger.getLogger( AttributeBuilder.class.getName() );

	/**
	 * The context for attribute building
	 */
	public static interface Context {
		public Type locateEntityTypeByName(String entityName);

		public void registerEmbeddedableType(EmbeddableTypeImpl embeddableType);

		public SessionFactoryImplementor getSessionFactory();

		public EntityPersister getSubClassEntityPersister(MappedSuperclassTypeImpl mappedSuperclass);

		public void handleUnsupportedFeature(UnsupportedFeature feature);
	}

	private final Context context;

	public AttributeBuilder(Context context) {
		this.context = context;
	}

	/**
	 * Build a normal attribute.
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param attributeBinding The Hibernate attribute binding descriptor
	 * @return The built attribute descriptor or null if the attribute is not part of the JPA 2 model (eg backrefs)
	 */
	public Attribute buildAttribute(AbstractManagedType ownerType, AttributeBinding attributeBinding) {
		if ( attributeBinding.getAttribute().isSynthetic() ) {
			// hide synthetic/virtual properties (fabricated by Hibernate) from the JPA metamodel.
			LOG.tracef(
					"Skipping synthetic property %s(%s)",
					ownerType.getJavaType().getName(),
					attributeBinding.getAttribute().getName()
			);
			return null;
		}
		LOG.trace("Building attribute [" + ownerType.getJavaType().getName() + "." + attributeBinding.getAttribute().getName() + "]");
		final AttributeMetadata attributeMetadata = determineAttributeMetadata(
				ownerType,
				attributeBinding,
				NORMAL_MEMBER_RESOLVER
		);
		if ( attributeMetadata == null ) {
			return null;
		}
		if ( attributeMetadata.isPlural() ) {
			return buildPluralAttribute( (PluralAttributeMetadata) attributeMetadata );
		}
		final SingularAttributeMetadata singularAttributeMetadata = (SingularAttributeMetadata)attributeMetadata;
		final Type metaModelType = getMetaModelType( singularAttributeMetadata.getAttributeTypeDescriptor() );
		//noinspection unchecked
		return new SingularAttributeImpl(
				attributeMetadata.getName(),
				attributeMetadata.getJavaType(),
				ownerType,
				attributeMetadata.getMember(),
				false,
				false,
				true, // todo : need to handle this somehow : property.isOptional(),
				metaModelType,
				attributeMetadata.getPersistentAttributeType()
		);
	}

	/**
	 * Build the identifier attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param attributeBinding The Hibernate attribute binding descriptor
	 *
	 * @return The built attribute descriptor
	 */
	@SuppressWarnings({ "unchecked" })
	public SingularAttributeImpl buildIdAttribute(AbstractIdentifiableType ownerType, AttributeBinding attributeBinding) {
        LOG.trace(
				"Building identifier attribute [" + ownerType.getJavaType().getName() + "#"
						+ attributeBinding.getAttribute().getName() + "]"
		);
		final SingularAttributeMetadata attributeMetadata = (SingularAttributeMetadata) determineAttributeMetadata(
				ownerType,
				attributeBinding,
				IDENTIFIER_MEMBER_RESOLVER
		);
		final Type metaModelType = getMetaModelType( attributeMetadata.getAttributeTypeDescriptor() );
		return new SingularAttributeImpl.Identifier(
				attributeBinding.getAttribute().getName(),
				attributeMetadata.getJavaType(),
				ownerType,
				attributeMetadata.getMember(),
				metaModelType,
				attributeMetadata.getPersistentAttributeType()
		);
	}

	/**
	 * Build the version attribute descriptor
	 *
	 * @param ownerType The descriptor of the attribute owner (aka declarer).
	 * @param attributeBinding The Hibernate attribute binding descriptor
	 *
	 * @return The built attribute descriptor
	 */
	@SuppressWarnings({ "unchecked" })
	public <X, Y> SingularAttributeImpl<X, Y> buildVersionAttribute(AbstractIdentifiableType<X> ownerType, AttributeBinding attributeBinding) {
        LOG.trace("Building version attribute [ownerType.getJavaType().getName()" + "." + "property.getName()]");
		final SingularAttributeMetadata<X,Y> attributeMetadata = (SingularAttributeMetadata<X, Y>) determineAttributeMetadata(
				ownerType,
				attributeBinding,
				VERSION_MEMBER_RESOLVER
		);
		final Type<Y> metaModelType = getMetaModelType( attributeMetadata.getAttributeTypeDescriptor() );
		return new SingularAttributeImpl.Version(
				attributeBinding.getAttribute().getName(),
				attributeMetadata.getJavaType(),
				ownerType,
				attributeMetadata.getMember(),
				metaModelType,
				attributeMetadata.getPersistentAttributeType()
		);
	}

	@SuppressWarnings( "unchecked" )
	private PluralAttribute buildPluralAttribute(PluralAttributeMetadata attributeMetadata) {
		final Type elementType = getMetaModelType( attributeMetadata.getElementAttributeTypeDescriptor() );
		if ( java.util.Map.class.isAssignableFrom( attributeMetadata.getJavaType() ) ) {
			final Type keyType = getMetaModelType( attributeMetadata.getMapKeyAttributeTypeDescriptor() );
			return PluralAttributeImpl.builder( attributeMetadata.getJavaType() )
					.owner( attributeMetadata.getOwnerType() )
					.elementType( elementType )
					.keyType( keyType )
					.member( attributeMetadata.getMember() )
					.binding( (PluralAttributeBinding) attributeMetadata.getAttributeBinding() )
					.persistentAttributeType( attributeMetadata.getPersistentAttributeType() )
					.build();
		}
        return PluralAttributeImpl.builder( attributeMetadata.getJavaType() )
				.owner( attributeMetadata.getOwnerType() )
				.elementType( elementType )
				.member( attributeMetadata.getMember() )
				.binding( (PluralAttributeBinding) attributeMetadata.getAttributeBinding() )
				.persistentAttributeType( attributeMetadata.getPersistentAttributeType() )
				.build();
	}

	@SuppressWarnings( "unchecked" )
	private <Y> Type<Y> getMetaModelType(AttributeTypeDescriptor attributeTypeDescriptor) {
		switch ( attributeTypeDescriptor.getValueClassification() ) {
			case BASIC: {
				return new BasicTypeImpl<Y>(
						attributeTypeDescriptor.getBindableType(),
						Type.PersistenceType.BASIC
				);
			}
			case ENTITY: {
				final org.hibernate.type.EntityType type = (EntityType) attributeTypeDescriptor.getHibernateType();
				return (Type<Y>) context.locateEntityTypeByName( type.getAssociatedEntityName() );
			}
			case EMBEDDABLE: {
				final EmbeddableTypeImpl<Y> embeddableType = new EmbeddableTypeImpl<Y>(
						attributeTypeDescriptor.getBindableType(),
						attributeTypeDescriptor.getAttributeMetadata().getOwnerType(),
						(ComponentType) attributeTypeDescriptor.getHibernateType()
				);
				context.registerEmbeddedableType( embeddableType );

				CompositeAttributeBinding compositeAttributeBinding =
						(CompositeAttributeBinding) attributeTypeDescriptor.getHibernateMetamodelType();
				for ( AttributeBinding subAttributeBinding : compositeAttributeBinding.attributeBindings() ) {
					final Attribute<Y, Object> attribute = buildAttribute( embeddableType, subAttributeBinding );
					if ( attribute != null ) {
						embeddableType.getBuilder().addAttribute( attribute );
					}
				}
				embeddableType.lock();
				return embeddableType;
			}
			default: {
				throw new AssertionFailure( "Unknown type : " + attributeTypeDescriptor.getValueClassification() );
			}
		}
	}

	private EntityMetamodel getDeclarerEntityMetamodel(IdentifiableType<?> ownerType) {
		final Type.PersistenceType persistenceType = ownerType.getPersistenceType();
		if ( persistenceType == Type.PersistenceType.ENTITY) {
			return context.getSessionFactory()
					.getEntityPersister( ownerType.getJavaType().getName() )
					.getEntityMetamodel();
		}
		else if ( persistenceType == Type.PersistenceType.MAPPED_SUPERCLASS) {
			return context.getSubClassEntityPersister( (MappedSuperclassTypeImpl) ownerType ).getEntityMetamodel();
		}
		else {
			throw new AssertionFailure( "Cannot get the metamodel for PersistenceType: " + persistenceType );
		}
	}

	/**
	 * Here is most of the nuts and bolts of this factory, where we interpret the known JPA metadata
	 * against the known Hibernate metadata and build a descriptor for the attribute.
	 *
	 * @param jpaOwner The JPA representation of the attribute owner
	 * @param attributeBinding Hibernate metamodel representation of the attribute binding
	 * @param memberResolver Strategy for how to resolve the member defining the attribute.
	 *
	 * @return The attribute description
	 */
	@SuppressWarnings({ "unchecked" })
	private AttributeMetadata determineAttributeMetadata(
			AbstractManagedType jpaOwner,
			AttributeBinding attributeBinding,
			MemberResolver memberResolver) {
        LOG.trace("Starting attribute metadata determination [" + attributeBinding.getAttribute().getName() + "]");
		final Member member = memberResolver.resolveMember( jpaOwner, attributeBinding );
        LOG.trace("    Determined member [" + member + "]");

		final org.hibernate.type.Type type = attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping();
        LOG.trace("    Determined type [name=" + type.getName() + ", class=" + type.getClass().getName() + "]");

		if ( type.isAnyType() ) {
			// ANY mappings are currently not supported in the JPA metamodel; see HHH-6589
			context.handleUnsupportedFeature( UnsupportedFeature.ANY );
			return null;
		}

		if ( SingularAttributeBinding.class.isInstance( attributeBinding ) ) {
			final SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;

			final PersistentAttributeType jpaAttributeType;
			if ( singularAttributeBinding.getAttribute().getSingularAttributeType().isComponent() ) {
				jpaAttributeType = PersistentAttributeType.EMBEDDED;
			}
			else if ( singularAttributeBinding.getAttribute().getSingularAttributeType().isAssociation() ) {
				jpaAttributeType = determineSingularAssociationAttributeType( member );
			}
			else {
				jpaAttributeType = PersistentAttributeType.BASIC;
			}
			return new SingularAttributeMetadataImpl( attributeBinding, jpaOwner, member, jpaAttributeType );
		}
		else {
			final PluralAttributeBinding pluralAttributeBinding = (PluralAttributeBinding) attributeBinding;

			// First, determine the type of the elements and use that to help determine the
			// collection type)
			final PluralAttributeElementBinding elementBinding = pluralAttributeBinding.getPluralAttributeElementBinding();
			final PluralAttributeElementNature elementNature = elementBinding.getPluralAttributeElementNature();
			final PersistentAttributeType persistentAttributeType;
			final PersistentAttributeType elementPersistentAttributeType;
			PersistentAttributeType keyPersistentAttributeType = null;

			if ( elementNature == PluralAttributeElementNature.MANY_TO_ANY ) {
				// ANY mappings are currently not supported in the JPA metamodel; see HHH-6589
				context.handleUnsupportedFeature( UnsupportedFeature.ANY );
				return null;
			}
			else if ( elementNature == PluralAttributeElementNature.BASIC ) {
				elementPersistentAttributeType = PersistentAttributeType.BASIC;
				persistentAttributeType = PersistentAttributeType.ELEMENT_COLLECTION;
			}
			else if ( elementNature == PluralAttributeElementNature.COMPOSITE ) {
				elementPersistentAttributeType = PersistentAttributeType.EMBEDDED;
				persistentAttributeType = PersistentAttributeType.ELEMENT_COLLECTION;
			}
			else {
				elementPersistentAttributeType = elementNature == PluralAttributeElementNature.MANY_TO_MANY
						? PersistentAttributeType.MANY_TO_MANY
						: PersistentAttributeType.ONE_TO_MANY;
				persistentAttributeType = elementPersistentAttributeType;
			}

			// For maps, also check the key binding
			if ( pluralAttributeBinding.getAttribute().getNature() == PluralAttributeNature.MAP ) {
				final IndexedPluralAttributeBinding indexedPluralAttributeBinding
						= (IndexedPluralAttributeBinding) pluralAttributeBinding;
				final PluralAttributeIndexNature indexNature
						= indexedPluralAttributeBinding.getPluralAttributeIndexBinding().getPluralAttributeIndexNature();

				if ( indexNature == PluralAttributeIndexNature.MANY_TO_ANY ) {
					context.handleUnsupportedFeature( UnsupportedFeature.ANY );
					return null;
				}
				else if ( indexNature == PluralAttributeIndexNature.MANY_TO_MANY ) {
					keyPersistentAttributeType = Attribute.PersistentAttributeType.MANY_TO_ONE;
				}
				else if ( indexNature == PluralAttributeIndexNature.COMPOSITE ) {
					keyPersistentAttributeType = Attribute.PersistentAttributeType.EMBEDDED;
				}
				else {
					keyPersistentAttributeType = Attribute.PersistentAttributeType.BASIC;
				}
			}
			else {
				// for the sake of symmetry...
				if ( pluralAttributeBinding.getPluralAttributeKeyBinding() != null ) {
					throw new HibernateException(
							String.format(
									"Encountered non-Map attribute binding with associated map-key binding : %s#%s",
									jpaOwner.getJavaType().getName(),
									pluralAttributeBinding.getAttribute().getName()
							)
					);
				}
			}

			return new PluralAttributeMetadataImpl(
					pluralAttributeBinding,
					jpaOwner,
					member,
					persistentAttributeType,
					elementPersistentAttributeType,
					keyPersistentAttributeType
			);
		}
	}

	public static PersistentAttributeType determineSingularAssociationAttributeType(Member member) {
		if ( Field.class.isInstance( member ) ) {
			return ( (Field) member ).getAnnotation( OneToOne.class ) != null
					? PersistentAttributeType.ONE_TO_ONE
					: PersistentAttributeType.MANY_TO_ONE;
		}
		else {
			return ( (Method) member ).getAnnotation( OneToOne.class ) != null
					? PersistentAttributeType.ONE_TO_ONE
					: PersistentAttributeType.MANY_TO_ONE;
		}
	}

	public static ParameterizedType getSignatureType(Member member) {
		final java.lang.reflect.Type type = Field.class.isInstance( member )
				? ( ( Field ) member ).getGenericType()
				: ( ( Method ) member ).getGenericReturnType();
		//this is a raw type
		if ( type instanceof Class ) return null;
		return (ParameterizedType) type;
	}

	public static PluralAttribute.CollectionType determineCollectionType(Class javaType) {
		if ( java.util.List.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.LIST;
		}
		else if ( java.util.Set.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.SET;
		}
		else if ( java.util.Map.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.MAP;
		}
		else if ( java.util.Collection.class.isAssignableFrom( javaType ) ) {
			return PluralAttribute.CollectionType.COLLECTION;
		}
		else {
			throw new IllegalArgumentException( "Expecting collection type [" + javaType.getName() + "]" );
		}
	}

//	public static boolean isManyToMany(Member member) {
//		return Field.class.isInstance( member )
//				? ( (Field) member ).getAnnotation( ManyToMany.class ) != null
//				: ( (Method) member ).getAnnotation( ManyToMany.class ) != null;
//	}

	private final MemberResolver EMBEDDED_MEMBER_RESOLVER = new MemberResolver() {
		@Override
		public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding) {
			final EmbeddableTypeImpl embeddableType = ( EmbeddableTypeImpl<?> ) owner;
			final String attributeName = attributeBinding.getAttribute().getName();
			return embeddableType.getHibernateType()
					.getComponentTuplizer()
					.getGetter( embeddableType.getHibernateType().getPropertyIndex( attributeName ) )
					.getMember();
		}
	};


	private final MemberResolver VIRTUAL_IDENTIFIER_MEMBER_RESOLVER = new MemberResolver() {
		@Override
		public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding) {
			final IdentifiableType identifiableType = (IdentifiableType) owner;
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			if ( ! entityMetamodel.getIdentifierProperty().isVirtual() ) {
				throw new IllegalArgumentException( "expecting IdClass mapping" );
			}
			org.hibernate.type.Type type = entityMetamodel.getIdentifierProperty().getType();
			if ( ! EmbeddedComponentType.class.isInstance( type ) ) {
				throw new IllegalArgumentException( "expecting IdClass mapping" );
			}

			final EmbeddedComponentType componentType = (EmbeddedComponentType) type;
			final String attributeName = attributeBinding.getAttribute().getName();
			return componentType.getComponentTuplizer()
					.getGetter( componentType.getPropertyIndex( attributeName ) )
					.getMember();
		}
	};

	/**
	 * A {@link Member} resolver for normal attributes.
	 */
	private final MemberResolver NORMAL_MEMBER_RESOLVER = new MemberResolver() {
		@Override
		public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding) {
			final Type.PersistenceType persistenceType = owner.getPersistenceType();
			if ( Type.PersistenceType.EMBEDDABLE == persistenceType ) {
				return EMBEDDED_MEMBER_RESOLVER.resolveMember( owner, attributeBinding );
			}
			else if ( Type.PersistenceType.ENTITY == persistenceType
					|| Type.PersistenceType.MAPPED_SUPERCLASS == persistenceType ) {
				final IdentifiableType identifiableType = (IdentifiableType) owner;
				final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
				final String propertyName = attributeBinding.getAttribute().getName();
				final Integer index = entityMetamodel.getPropertyIndexOrNull( propertyName );
				if ( index == null ) {
					// just like in #determineIdentifierJavaMember , this *should* indicate we have an IdClass mapping
					return VIRTUAL_IDENTIFIER_MEMBER_RESOLVER.resolveMember( owner, attributeBinding );
				}
				else {
					return entityMetamodel.getTuplizer()
							.getGetter( index )
							.getMember();
				}
			}
			else {
				throw new IllegalArgumentException( "Unexpected owner type : " + persistenceType );
			}
		}
	};

	private final MemberResolver IDENTIFIER_MEMBER_RESOLVER = new MemberResolver() {
		@Override
		public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding) {
			final IdentifiableType identifiableType = (IdentifiableType) owner;
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			final String attributeName = attributeBinding.getAttribute().getName();
			if ( ! attributeName.equals( entityMetamodel.getIdentifierProperty().getName() ) ) {
				// this *should* indicate processing part of an IdClass...
				return VIRTUAL_IDENTIFIER_MEMBER_RESOLVER.resolveMember( owner, attributeBinding );
			}
			return entityMetamodel.getTuplizer().getIdentifierGetter().getMember();
		}
	};

	private final MemberResolver VERSION_MEMBER_RESOLVER = new MemberResolver() {
		@Override
		public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding) {
			final IdentifiableType identifiableType = (IdentifiableType) owner;
			final EntityMetamodel entityMetamodel = getDeclarerEntityMetamodel( identifiableType );
			final String versionPropertyName = attributeBinding.getAttribute().getName();
			if ( ! versionPropertyName.equals( entityMetamodel.getVersionProperty().getName() ) ) {
				// this should never happen, but to be safe...
				throw new IllegalArgumentException( "Given property did not match declared version property" );
			}
			return entityMetamodel.getTuplizer().getVersionGetter().getMember();
		}
	};
}
