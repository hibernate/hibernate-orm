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

import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.type.Type;

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
				resolveAttributeTypeInformation( attributeBinding );
			}
		}
	}

	// perform any needed type resolutions for discriminator
	private void resolveDiscriminatorTypeInformation(EntityDiscriminator discriminator) {
		// perform any needed type resolutions for discriminator
		Type resolvedHibernateType = determineHibernateTypeFromDescriptor( discriminator.getExplicitHibernateTypeDescriptor() );
		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDownIfNeeded(
					discriminator.getExplicitHibernateTypeDescriptor(),
					discriminator.getBoundValue(),
					resolvedHibernateType
			);
		}
	}

	// perform any needed type resolutions
	private void resolveAttributeTypeInformation(AttributeBinding attributeBinding) {

		// we can determine the Hibernate Type if either:
		// 		1) the user explicitly named a Type in a HibernateTypeDescriptor
		// 		2) we know the java type of the attribute
		Type resolvedHibernateType = determineHibernateTypeFromDescriptor( attributeBinding.getHibernateTypeDescriptor() );
		if ( resolvedHibernateType == null ) {
			resolvedHibernateType = determineHibernateTypeFromAttribute( attributeBinding.getAttribute() );
		}
		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDownIfNeeded( attributeBinding, resolvedHibernateType );
		}
	}

	private Type determineHibernateTypeFromDescriptor(HibernateTypeDescriptor hibernateTypeDescriptor) {
		if ( hibernateTypeDescriptor.getResolvedTypeMapping() != null ) {
			return hibernateTypeDescriptor.getResolvedTypeMapping();
		}
		String typeName = null;
		Properties typeParameters = new Properties( );
		if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
			typeName = hibernateTypeDescriptor.getExplicitTypeName();
			if ( hibernateTypeDescriptor.getTypeParameters() != null ) {
				typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
			}
		}
		else if ( hibernateTypeDescriptor.getJavaTypeName() != null ) {
			typeName = hibernateTypeDescriptor.getJavaTypeName();
		}
		return getHeuristicType( typeName, typeParameters );
	}

	private Type determineHibernateTypeFromAttribute(Attribute attribute) {
		// currently, we only handle singular attributes
		if ( attribute.isSingular() ) {
			final SingularAttribute singularAttribute = ( SingularAttribute ) attribute;
			if ( singularAttribute.getSingularAttributeType() != null ) {
				 return getHeuristicType(
						singularAttribute.getSingularAttributeType().getClassName(), new Properties()
				);
			}
		}
		return null;
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

	private void pushHibernateTypeInformationDownIfNeeded(AttributeBinding attributeBinding, Type resolvedHibernateType) {

		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		Value value = null;
		if ( SingularAttribute.class.isInstance( attributeBinding.getAttribute() ) ) {
			value = SingularAttributeBinding.class.cast( attributeBinding ).getValue();
			final SingularAttribute singularAttribute = (SingularAttribute) attributeBinding.getAttribute();
			if ( ! singularAttribute.isTypeResolved() ) {
				if ( hibernateTypeDescriptor.getJavaTypeName() != null ) {
					singularAttribute.resolveType( metadata.makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
				}
			}
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
