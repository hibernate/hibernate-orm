/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.spi.source.MappingException;

/**
 * Base class for any and all source objects coming from {@code hbm.xml} parsing.  Defines standard access
 * back to the {@link MappingDocument} object and the services it provides (namely access to
 * {@link HbmBindingContext}).
 *
 * @author Steve Ebersole
 */
public abstract class AbstractHbmSourceNode {
	private final MappingDocument sourceMappingDocument;

	protected AbstractHbmSourceNode(MappingDocument sourceMappingDocument) {
		this.sourceMappingDocument = sourceMappingDocument;
	}

	protected MappingDocument sourceMappingDocument() {
		return sourceMappingDocument;
	}

	protected HbmBindingContext bindingContext() {
		return sourceMappingDocument().getMappingLocalBindingContext();
	}

	protected Origin origin() {
		return sourceMappingDocument().getOrigin();
	}

	protected JaxbRoot<JaxbHibernateMapping> mappingRoot() {
		return sourceMappingDocument().getJaxbRoot();
	}

	protected ValueHolder<Class<?>> makeClassReference(String className) {
		return bindingContext().makeClassReference( bindingContext().qualifyClassName( className ) );
	}

	protected MappingException makeMappingException(String message) {
		return bindingContext().makeMappingException( message );
	}

	protected MappingException makeMappingException(String message, Exception cause) {
		return bindingContext().makeMappingException( message, cause );
	}
}
