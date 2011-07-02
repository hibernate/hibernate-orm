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
package org.hibernate.metamodel.binder.source.hbm;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binder.MappingException;
import org.hibernate.metamodel.binder.source.JoinedSubClassEntityDescriptor;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclassElement;

import static org.hibernate.metamodel.binding.InheritanceType.JOINED;

/**
 * Unified descriptor for (SQL) union-based inheritance strategies.
 *
 * @author Steve Ebersole
 */
public class UnionSubClassEntityDescriptorImpl
		extends AbstractEntityDescriptorImpl
		implements JoinedSubClassEntityDescriptor {

	/**
	 * This form used when an explicit {@code extends} attribute names this mapping's super entity.
	 *
	 * @param entityClazz The JAXB entity mapping
	 * @param bindingContext The context for the binding process.
	 */
	public UnionSubClassEntityDescriptorImpl(
			EntityElement entityClazz,
			HbmBindingContext bindingContext) {
		this( entityClazz, extractExtendsName( entityClazz, bindingContext ), bindingContext );
	}

	private static String extractExtendsName(EntityElement entityClazz, HbmBindingContext bindingContext) {
		final String extendsName = ( (XMLUnionSubclassElement) entityClazz ).getExtends();
		if ( StringHelper.isEmpty( extendsName ) ) {
			throw new MappingException(
					"Subclass entity mapping [" + bindingContext.determineEntityName( entityClazz )
							+ "] was not contained in super entity mapping",
					bindingContext.getOrigin()
			);
		}
		return extendsName;
	}

	/**
	 * This form would be used when the subclass definition if nested within its super type mapping.
	 *
	 * @param entityClazz The JAXB entity mapping
	 * @param superEntityName The name of the containing (and thus super) entity
	 * @param bindingContext The context for the binding process.
	 */
	public UnionSubClassEntityDescriptorImpl(
			EntityElement entityClazz,
			String superEntityName,
			HbmBindingContext bindingContext) {
		super( entityClazz, superEntityName, JOINED, bindingContext );
	}
}
