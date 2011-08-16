/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.source.internal;

import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.binding.AbstractCollectionElement;
import org.hibernate.metamodel.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.BasicCollectionElement;
import org.hibernate.metamodel.binding.CollectionElementNature;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * This is a TEMPORARY way to initialize Hibernate types.
 * This class will be removed when types are resolved properly.
 *
 * @author Gail Badner
 */
class HibernateTypeResolver {

	private final MetadataImplementor metadata;

	HibernateTypeResolver(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	void resolve() {
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.getHierarchyDetails().getEntityDiscriminator() != null ) {
				resolveDiscriminatorTypeInformation( entityBinding.getHierarchyDetails().getEntityDiscriminator() );
			}
			for ( AttributeBinding attributeBinding : entityBinding.attributeBindings() ) {
				if ( SingularAttributeBinding.class.isInstance( attributeBinding ) ) {
					resolveSingularAttributeTypeInformation(
							SingularAttributeBinding.class.cast( attributeBinding  )
					);
				}
				else if ( AbstractPluralAttributeBinding.class.isInstance( attributeBinding ) ) {
					resolvePluralAttributeTypeInformation(
							AbstractPluralAttributeBinding.class.cast( attributeBinding )
					);
				}
				else {
					throw new AssertionFailure( "Unknown type of AttributeBinding: " + attributeBinding.getClass().getName() );
				}
			}
		}
	}

	// perform any needed type resolutions for discriminator
	private void resolveDiscriminatorTypeInformation(EntityDiscriminator discriminator) {
		// perform any needed type resolutions for discriminator
		Type resolvedHibernateType = determineSingularTypeFromDescriptor( discriminator.getExplicitHibernateTypeDescriptor() );
		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDownIfNeeded(
					discriminator.getExplicitHibernateTypeDescriptor(),
					discriminator.getBoundValue(),
					resolvedHibernateType
			);
		}
	}

	private Type determineSingularTypeFromDescriptor(HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			return hibernateTypeDescriptor.getResolvedTypeMapping();
		}
		String typeName = determineTypeName( hibernateTypeDescriptor );
		Properties typeParameters = getTypeParameters( hibernateTypeDescriptor );
		return getHeuristicType( typeName, typeParameters );
	}

	private static String determineTypeName(HibernateTypeDescriptor hibernateTypeDescriptor) {
		return hibernateTypeDescriptor.getExplicitTypeName() != null
				? hibernateTypeDescriptor.getExplicitTypeName()
				: hibernateTypeDescriptor.getJavaTypeName();
	}

	private static Properties getTypeParameters(HibernateTypeDescriptor hibernateTypeDescriptor) {
		Properties typeParameters = new Properties( );
		if ( hibernateTypeDescriptor.getTypeParameters() != null ) {
			typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
		}
		return typeParameters;
	}

	// perform any needed type resolutions for SingularAttributeBinding
	private void resolveSingularAttributeTypeInformation(SingularAttributeBinding attributeBinding) {
		if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() != null ) {
			return;
		}
		// we can determine the Hibernate Type if either:
		// 		1) the user explicitly named a Type in a HibernateTypeDescriptor
		// 		2) we know the java type of the attribute
		Type resolvedType;
		resolvedType = determineSingularTypeFromDescriptor( attributeBinding.getHibernateTypeDescriptor() );
		if ( resolvedType == null ) {
			if ( ! attributeBinding.getAttribute().isSingular() ) {
				throw new AssertionFailure( "SingularAttributeBinding object has a plural attribute: " + attributeBinding.getAttribute().getName() );
			}
			final SingularAttribute singularAttribute = ( SingularAttribute ) attributeBinding.getAttribute();
			if ( singularAttribute.getSingularAttributeType() != null ) {
				resolvedType = getHeuristicType(
						singularAttribute.getSingularAttributeType().getClassName(), new Properties()
				);
			}
		}
		if ( resolvedType != null ) {
			pushHibernateTypeInformationDownIfNeeded( attributeBinding, resolvedType );
		}
	}

	// perform any needed type resolutions for PluralAttributeBinding
	private void resolvePluralAttributeTypeInformation(AbstractPluralAttributeBinding attributeBinding) {
		if ( attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping() != null ) {
			return;
		}
		Type resolvedType;
		// do NOT look at java type...
		//String typeName = determineTypeName( attributeBinding.getHibernateTypeDescriptor() );
		String typeName = attributeBinding.getHibernateTypeDescriptor().getExplicitTypeName();
		if ( typeName != null ) {
			resolvedType =
					metadata.getTypeResolver()
							.getTypeFactory()
							.customCollection(
									typeName,
									getTypeParameters( attributeBinding.getHibernateTypeDescriptor() ),
									attributeBinding.getAttribute().getName(),
									attributeBinding.getReferencedPropertyName(),
									attributeBinding.getCollectionElement().getCollectionElementNature() ==
											CollectionElementNature.COMPOSITE
							);
		}
		else {
			resolvedType = determineDefaultCollectionInformation( attributeBinding );
		}
		if ( resolvedType != null ) {
			pushHibernateTypeInformationDownIfNeeded(
					attributeBinding.getHibernateTypeDescriptor(),
					null,
					resolvedType );
		}
		resolveCollectionElementTypeInformation( attributeBinding.getCollectionElement() );
	}

	private Type determineDefaultCollectionInformation(AbstractPluralAttributeBinding attributeBinding) {
		final TypeFactory typeFactory = metadata.getTypeResolver().getTypeFactory();
		switch ( attributeBinding.getAttribute().getNature() ) {
			case SET: {
				return typeFactory.set(
						attributeBinding.getAttribute().getName(),
						attributeBinding.getReferencedPropertyName(),
						attributeBinding.getCollectionElement().getCollectionElementNature() == CollectionElementNature.COMPOSITE
				);
			}
			case BAG: {
				return typeFactory.bag(
						attributeBinding.getAttribute().getName(),
						attributeBinding.getReferencedPropertyName(),
						attributeBinding.getCollectionElement()
								.getCollectionElementNature() == CollectionElementNature.COMPOSITE
				);
			}
			default: {
				throw new UnsupportedOperationException(
						"Collection type not supported yet:" + attributeBinding.getAttribute().getNature()
				);
			}
		}
	}

	private void resolveCollectionElementTypeInformation(AbstractCollectionElement collectionElement) {
		switch ( collectionElement.getCollectionElementNature() ) {
			case BASIC: {
				resolveBasicCollectionElement( BasicCollectionElement.class.cast( collectionElement ) );
				break;
			}
			case COMPOSITE:
			case ONE_TO_MANY:
			case MANY_TO_MANY:
			case MANY_TO_ANY: {
				throw new UnsupportedOperationException( "Collection element nature not supported yet: " + collectionElement.getCollectionElementNature() );
			}
			default: {
				throw new AssertionFailure( "Unknown collection element nature : " + collectionElement.getCollectionElementNature() );
			}
		}
	}

	private void resolveBasicCollectionElement(BasicCollectionElement basicCollectionElement) {
		Type resolvedHibernateType = determineSingularTypeFromDescriptor( basicCollectionElement.getHibernateTypeDescriptor() );
		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDownIfNeeded(
					basicCollectionElement.getHibernateTypeDescriptor(),
					basicCollectionElement.getElementValue(),
					resolvedHibernateType
			);
		}
	}

	private Type getHeuristicType(String typeName, Properties typeParameters) {
		if ( typeName != null ) {
			try {
				return metadata.getTypeResolver().heuristicType( typeName, typeParameters );
			}
			catch (Exception ignore) {
			}
		}

		return null;
	}

	private void pushHibernateTypeInformationDownIfNeeded(SingularAttributeBinding attributeBinding, Type resolvedHibernateType) {

		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		final SingularAttribute singularAttribute = SingularAttribute.class.cast( attributeBinding.getAttribute() );
		final Value value = attributeBinding.getValue();
		if ( ! singularAttribute.isTypeResolved() && hibernateTypeDescriptor.getJavaTypeName() != null ) {
			singularAttribute.resolveType( metadata.makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		}

		// sql type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.pushHibernateTypeInformationDownIfNeeded(
				hibernateTypeDescriptor, value, resolvedHibernateType
		);
	}

	private void pushHibernateTypeInformationDownIfNeeded(
			HibernateTypeDescriptor hibernateTypeDescriptor,
			Value value,
			Type resolvedHibernateType) {
		if ( resolvedHibernateType == null ) {
			return;
		}
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() == null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedHibernateType );
		}

		// java type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
			hibernateTypeDescriptor.setJavaTypeName( resolvedHibernateType.getReturnedClass().getName() );
		}

	   // todo : this can be made a lot smarter, but for now this will suffice.  currently we only handle single value bindings

	   if ( SimpleValue.class.isInstance( value ) ) {
		   SimpleValue simpleValue = ( SimpleValue ) value;
		   if ( simpleValue.getDatatype() == null ) {
			   simpleValue.setDatatype(
					   new Datatype(
							   resolvedHibernateType.sqlTypes( metadata )[0],
							   resolvedHibernateType.getName(),
							   resolvedHibernateType.getReturnedClass()
					   )
			   );
		   }
	   }
	}
}
