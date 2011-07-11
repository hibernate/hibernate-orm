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

import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.domain.AbstractAttributeContainer;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.BasicType;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.relational.Datatype;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Value;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.type.AbstractStandardBasicType;
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
			for ( AttributeBinding attributeBinding : entityBinding.getAttributeBindings() ) {
				Type type = resolveHibernateType( attributeBinding );
				if ( type != null && ! type.isAssociationType() && ! type.isCollectionType() && ! type.isComponentType() ) {
					resolveJavaType( attributeBinding.getAttribute(), type );
					for ( Value value : attributeBinding.getValues() ) {
						resolveSqlType( value, type );
					}
				}
			}
		}
	}

	private Type resolveHibernateType(AttributeBinding attributeBinding) {
		if ( attributeBinding.getHibernateTypeDescriptor().getExplicitType() != null ) {
			return attributeBinding.getHibernateTypeDescriptor().getExplicitType(); // already resolved
		}

		// this only works for "basic" attribute types
		HibernateTypeDescriptor typeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		if ( typeDescriptor == null || typeDescriptor.getTypeName() == null) {
			throw new MappingException( "Hibernate type name has not been defined for attribute: " +
					getQualifiedAttributeName( attributeBinding )
			);
		}
		Type type = null;
		if ( typeDescriptor.getTypeName() != null ) {
			Properties typeParameters = null;
			if ( typeDescriptor.getTypeParameters() != null ) {
				typeParameters = new Properties();
				typeParameters.putAll( typeDescriptor.getTypeParameters() );
			}
			type = metadata.getTypeResolver().heuristicType(
							typeDescriptor.getTypeName(),
							typeParameters
					);
			typeDescriptor.setExplicitType( type );
		}
		return type;
	}

	// this only works for singular basic types
	private void resolveJavaType(Attribute attribute, Type type) {
		if ( ! ( type instanceof AbstractStandardBasicType ) || ! attribute.isSingular() ) {
			return;
		}
		// Converting to SingularAttributeImpl is bad, but this resolver is TEMPORARY!
		AbstractAttributeContainer.SingularAttributeImpl singularAttribute =
				( AbstractAttributeContainer.SingularAttributeImpl ) attribute;
		if ( ! singularAttribute.isTypeResolved() ) {
			singularAttribute.resolveType(
					new BasicType(
							new JavaType( ( ( AbstractStandardBasicType) type ).getJavaTypeDescriptor().getJavaTypeClass() )
					)
			);
		}
	}

	// this only works for singular basic types
	private void resolveSqlType(Value value, Type type) {
		if ( value == null || ! ( value instanceof SimpleValue ) || ! ( type instanceof AbstractStandardBasicType )  ) {
			return;
		}
		// Converting to AbstractStandardBasicType is bad, but this resolver is TEMPORARY!
		AbstractStandardBasicType basicType = ( AbstractStandardBasicType ) type;
		Datatype dataType = new Datatype(
								basicType.getSqlTypeDescriptor().getSqlType(),
								basicType.getName(),
								basicType.getReturnedClass()
						);
		( (SimpleValue) value ).setDatatype( dataType );
	}

	// TODO: this does not work for components
	private static String getQualifiedAttributeName(AttributeBinding attributebinding) {
		return new StringBuilder()
				.append( attributebinding.getEntityBinding().getEntity().getJavaType().getName() )
				.append( "." )
				.append( attributebinding.getAttribute().getName() )
				.toString();
	}
}
