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

package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import java.util.Map;

/**
 * Determines explicit Hibernate type information for JPA mapped attributes when additional type information is
 * provided via annotations like {@link javax.persistence.Lob}, {@link javax.persistence.Enumerated} and
 * {@link javax.persistence.Temporal}.
 *
 * @author Strong Liu
 */
public interface AttributeTypeResolver {
	/**
	 * @return Returns an explicit hibernate type name in case the mapped attribute has an additional
	 *         {@link org.hibernate.annotations.Type} annotation or an implicit type is given via the use of annotations like
	 *         {@link javax.persistence.Lob}, {@link javax.persistence.Enumerated} and
	 *         {@link javax.persistence.Temporal}.  If no annotated types are
	 *         available, checks for type definitions in
	 *         {@link org.hibernate.annotations.TypeDef}.  Returns null if none of the
	 *         above are found.
	 */
	String getExplicitHibernateTypeName();
	
	/**
	 * @return Returns the same type name as
	 * {@link #getExplicitHibernateTypeName}, but skips the
	 * {@link org.hibernate.annotations.TypeDef} check.  This is mainly for
	 * {@link CompositeAttributeTypeResolver}, as it needs to check the
	 * {@link org.hibernate.annotations.TypeDef} only once at the very end if nothing
	 * is returned by the list of resolvers.
	 */
	String getExplicitAnnotatedHibernateTypeName();

	/**
	 * @return Returns a map of optional type parameters. See {@link #getExplicitHibernateTypeName()}.
	 */
	Map<String, String> getExplicitHibernateTypeParameters();
}
