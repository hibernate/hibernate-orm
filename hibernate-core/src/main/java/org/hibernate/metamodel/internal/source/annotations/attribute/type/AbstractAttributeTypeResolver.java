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

package org.hibernate.metamodel.internal.source.annotations.attribute.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public abstract class AbstractAttributeTypeResolver implements AttributeTypeResolver {
	
	protected final MappedAttribute mappedAttribute;

	public AbstractAttributeTypeResolver( MappedAttribute mappedAttribute ) {
		this.mappedAttribute = mappedAttribute;
	}
	
	@Override
	final public String getExplicitHibernateTypeName() {
		String type = getExplicitAnnotatedHibernateTypeName();
		// If the attribute is annotated with a type, use it.  Otherwise,
		// check for a @TypeDef.
		if ( !StringHelper.isEmpty( type ) ) {
			return type;
		} else {
			return hasEntityTypeDef() ? mappedAttribute.getAttributeType().getName() : null;
		}
	}
	
	@Override
	final public String getExplicitAnnotatedHibernateTypeName() {
		return resolveAnnotatedHibernateTypeName( 
				getTypeDeterminingAnnotationInstance() );
	}

	@Override
	final public Map<String, String> getExplicitHibernateTypeParameters() {
		Map<String, String> result = new HashMap<String, String>(  );
		//this is only use by enum type and serializable blob type, but we put there anyway
		result.put(
				DynamicParameterizedType.RETURNED_CLASS,
				mappedAttribute.getAttributeType().getName()
		);
		if ( StringHelper.isNotEmpty( getExplicitHibernateTypeName() ) ) {
			result.putAll( resolveHibernateTypeParameters(
					getTypeDeterminingAnnotationInstance() ) );
		}
		return result;
	}
	
	final protected boolean hasEntityTypeDef() {
		return mappedAttribute.getContext()
				.getMetadataImplementor().hasTypeDefinition( 
						mappedAttribute.getAttributeType().getName() );
	}
	
	protected abstract AnnotationInstance getTypeDeterminingAnnotationInstance();

	protected abstract String resolveAnnotatedHibernateTypeName(AnnotationInstance annotationInstance);

	protected Map<String, String> resolveHibernateTypeParameters(AnnotationInstance annotationInstance) {
		return Collections.emptyMap();
	}
}
