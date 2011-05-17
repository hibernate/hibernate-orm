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

import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * @author Gail Badner
 */
public class JavaClassNameResolver {
	private final MetadataImplementor metadata;

	JavaClassNameResolver(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	public void resolve(EntityBinding entityBinding) {
		// TODO: this only deals w/ POJO...
		Entity entity = entityBinding.getEntity();
		if ( entity == null ) {
			throw new MappingException(
					"Cannot resolve Java type names because the domain model has not been bound to EntityBinding." );
		}
		String entityClassName =
				entity.getPojoEntitySpecifics().getClassName() != null ?
						entity.getPojoEntitySpecifics().getClassName() :
						entity.getPojoEntitySpecifics().getProxyInterfaceName();
		if ( entityClassName == null ) {
			throw new MappingException( "No Java class or interface defined for: " + entityBinding.getEntity().getName() );
		}

		for ( Attribute attribute : entity.getAttributes() ) {
			if ( attribute.isSingular() ) {
				SingularAttribute singularAttribute = SingularAttribute.class.cast( attribute );
				if ( singularAttribute.getSingularAttributeType().getName() == null ) {

				}
			}
		}

	}


}
