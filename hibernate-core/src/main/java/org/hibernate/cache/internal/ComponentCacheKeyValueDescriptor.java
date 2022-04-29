/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * CacheKeyValueDescriptor used to describe normal composite mappings
 *
 * @see CustomComponentCacheKeyValueDescriptor
 */
public class ComponentCacheKeyValueDescriptor implements CacheKeyValueDescriptor {
	private final NavigableRole role;
	private final SessionFactoryImplementor sessionFactory;

	private transient EmbeddableValuedModelPart embeddedMapping;

	public ComponentCacheKeyValueDescriptor(NavigableRole role, SessionFactoryImplementor sessionFactory) {
		this.role = role;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public int getHashCode(Object key) {
		final List<AttributeMapping> attrs = getEmbeddedMapping().getEmbeddableTypeDescriptor().getAttributeMappings();
		int result = 17;

		for ( int i = 0; i < attrs.size(); i++ ) {
			final AttributeMapping attr = attrs.get( i );
			final Object attrValue = getAttributeValue( key, i, attr );
			result *= 37;
			if ( attrValue != null ) {
				//noinspection rawtypes
				final JavaType javaType = attr.getJavaType();
				//noinspection unchecked
				result += javaType.extractHashCode( attrValue );
			}
		}

		return result;
	}

	@Override
	public boolean isEqual(Object key1, Object key2) {
		if ( key1 == key2 ) {
			return true;
		}

		final List<AttributeMapping> attrs = getEmbeddedMapping().getEmbeddableTypeDescriptor().getAttributeMappings();
		for ( int i = 0; i < attrs.size(); i++ ) {
			final AttributeMapping attr = attrs.get( i );

			final Object value1 = getAttributeValue( key1, i, attr );
			final Object value2 = getAttributeValue( key2, i, attr );

			//noinspection unchecked
			final JavaType<Object> javaType = (JavaType<Object>) attr.getJavaType();
			if ( ! javaType.areEqual( value1, value2 ) ) {
				return false;
			}
		}

		return true;
	}

	public Object getAttributeValue(Object component, int i, AttributeMapping attr) {
		if ( component instanceof Object[] ) {
			return ( (Object[]) component )[i];
		}
		else {
			return attr.getValue( component );
		}
	}


	private EmbeddableValuedModelPart getEmbeddedMapping() {
		if ( embeddedMapping == null ) {
			embeddedMapping = sessionFactory.getRuntimeMetamodels().getEmbedded( role );
		}
		return embeddedMapping;
	}
}
