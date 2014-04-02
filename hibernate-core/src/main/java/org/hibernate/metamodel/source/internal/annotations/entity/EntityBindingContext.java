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
package org.hibernate.metamodel.source.internal.annotations.entity;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.BaseDelegatingBindingContext;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;


/**
 * Annotation version of a local binding context.
 * 
 * @author Steve Ebersole
 */
public class EntityBindingContext
		extends BaseDelegatingBindingContext
		implements LocalBindingContext, AnnotationBindingContext {
	private final AnnotationBindingContext contextDelegate;
	private final Origin origin;

	public EntityBindingContext(AnnotationBindingContext contextDelegate, ManagedTypeMetadata source) {
		super( contextDelegate );
		this.contextDelegate = contextDelegate;
		this.origin = new Origin( SourceType.ANNOTATION, source.getName() );
	}

	public AnnotationBindingContext getBaseContext() {
		return contextDelegate;
	}

	@Override
	public Origin getOrigin() {
		return origin;
	}

	@Override
	public MappingException makeMappingException(String message) {
		return new AnnotationException( message, getOrigin() );
	}

	@Override
	public MappingException makeMappingException(String message, Exception cause) {
		return new AnnotationException( message, cause, getOrigin() );
	}

	@Override
	public IdentifierGeneratorDefinition findIdGenerator(String name) {
		return contextDelegate.findIdGenerator( name );
	}
}
