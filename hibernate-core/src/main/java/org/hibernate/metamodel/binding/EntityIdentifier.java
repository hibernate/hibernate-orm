/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EntityIdentifier {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityIdentifier.class.getName());

	private final EntityBinding entityBinding;
	private AttributeBinding attributeBinding;
	// todo : generator, mappers, etc

	/**
	 * Create an identifier
	 * @param entityBinding
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public AttributeBinding getValueBinding() {
		return attributeBinding;
	}

	public void setValueBinding(AttributeBinding attributeBinding) {
		if ( this.attributeBinding != null ) {
			// todo : error?  or just log?  for now just log
			LOG.entityIdentifierValueBindingExists( entityBinding.getEntity().getName() );
		}
		this.attributeBinding = attributeBinding;
	}
}
