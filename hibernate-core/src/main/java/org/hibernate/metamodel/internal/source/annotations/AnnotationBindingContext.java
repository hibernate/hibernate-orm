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
package org.hibernate.metamodel.internal.source.annotations;

import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.BindingContext;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.TypeResolver;

/**
 * Defines an interface for providing additional annotation related context information.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public interface AnnotationBindingContext extends BindingContext {
	/**
	 * The annotation repository that this context know about.
	 *
	 * @return The {@link IndexView} that this context know about.
	 */
	IndexView getIndex();

	/**
	 * Gets the class (or interface, or annotation) that was scanned during the
	 * indexing phase.
	 *
	 * @param className the name of the class
	 * @return information about the class or null if it is not known
	 */
	ClassInfo getClassInfo(String className);

	/**
	 * Gets the {@literal ClassMate} {@link TypeResolver} used in this context.
	 *
	 * @return The {@link TypeResolver} associated within this context.
	 */
	TypeResolver getTypeResolver();

	/**
	 * Gets the {@literal ClassMate} {@link MemberResolver} used in this context.
	 *
	 * @return The {@link MemberResolver} associated within this context.
	 */
	MemberResolver getMemberResolver();


	IdentifierGeneratorDefinition findIdGenerator(String name);
}
