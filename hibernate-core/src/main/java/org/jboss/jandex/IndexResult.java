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
package org.jboss.jandex;

import java.util.Collection;

/**
 * The basic contract for accessing Jandex indexed information.
 *
 * @author Jason Greene
 * @author Steve Ebersole
 */
public interface IndexResult {
	public Collection<ClassInfo> getKnownClasses();

	public ClassInfo getClassByName(DotName className);

	public Collection<ClassInfo> getKnownDirectSubclasses(DotName className);

	/**
	 * Returns all known (including non-direct) sub classes of the given class.  I.e., returns all known classes
	 * that are assignable to the given class.
	 *
	 * @param className The class
	 *
	 * @return All known subclasses
	 */
	public Collection<ClassInfo> getAllKnownSubclasses(final DotName className);

	public Collection<ClassInfo> getKnownDirectImplementors(DotName className);

	public Collection<ClassInfo> getAllKnownImplementors(final DotName interfaceName);

	public Collection<AnnotationInstance> getAnnotations(DotName annotationName);
}
