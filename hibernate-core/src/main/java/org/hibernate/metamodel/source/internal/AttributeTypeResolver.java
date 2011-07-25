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
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.type.Type;

/**
 * This is a TEMPORARY way to initialize HibernateTypeDescriptor.explicitType.
 * This class will be removed when types are resolved properly.
 *
 * @author Gail Badner
 */
class AttributeTypeResolver {

	private final MetadataImplementor metadata;

	AttributeTypeResolver(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	void resolve() {
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			for ( AttributeBinding attributeBinding : entityBinding.attributeBindings() ) {
				resolveTypeInformation( attributeBinding );
			}
		}
	}

	private void resolveTypeInformation(AttributeBinding attributeBinding) {
		// perform any needed type resolutions

		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();

		Type resolvedHibernateType = hibernateTypeDescriptor.getResolvedTypeMapping();
		if ( resolvedHibernateType == null ) {
			resolvedHibernateType = determineHibernateType( attributeBinding );
			if ( resolvedHibernateType != null ) {
				hibernateTypeDescriptor.setResolvedTypeMapping( resolvedHibernateType );
			}
		}

		if ( resolvedHibernateType != null ) {
			pushHibernateTypeInformationDownIfNeeded( attributeBinding, resolvedHibernateType );
		}
	}

	private Type determineHibernateType(AttributeBinding attributeBinding) {
		String typeName = null;
		Properties typeParameters = new Properties();

		// we can determine the Hibernate Type if either:
		// 		1) the user explicitly named a Type
		// 		2) we know the java type of the attribute

		if ( attributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			typeName = attributeBinding.getHibernateTypeDescriptor().getExplicitTypeName();
			if ( attributeBinding.getHibernateTypeDescriptor().getTypeParameters() != null ) {
				typeParameters.putAll( attributeBinding.getHibernateTypeDescriptor().getTypeParameters() );
			}
		}
		else {
			typeName = attributeBinding.getHibernateTypeDescriptor().getJavaTypeName();
			if ( typeName == null ) {
				if ( attributeBinding.getAttribute().isSingular() ) {
					SingularAttribute singularAttribute = (SingularAttribute) attributeBinding.getAttribute();
					if ( singularAttribute.getSingularAttributeType() != null ) {
						typeName = singularAttribute.getSingularAttributeType().getClassName();
					}
				}
			}
		}

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

		// java type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
			hibernateTypeDescriptor.setJavaTypeName( resolvedHibernateType.getReturnedClass().getName() );
		}

		if ( SingularAttribute.class.isInstance( attributeBinding.getAttribute() ) ) {
			final SingularAttribute singularAttribute = (SingularAttribute) attributeBinding.getAttribute();
			if ( ! singularAttribute.isTypeResolved() ) {
				if ( hibernateTypeDescriptor.getJavaTypeName() != null ) {
					singularAttribute.resolveType( metadata.makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
				}
			}
		}


		// sql type information ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		// todo : this can be made a lot smarter, but for now this will suffice.  currently we only handle single value bindings

		if ( SingularAttribute.class.isInstance( attributeBinding.getAttribute() ) ) {
			final Value value = SingularAttributeBinding.class.cast( attributeBinding ).getValue();
			if ( SimpleValue.class.isInstance( value ) ) {
				SimpleValue simpleValue = (SimpleValue) value;
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

}
