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
package org.hibernate.metamodel.source.annotations;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.DotName;

/**
 * Defines the dot names for the JPA annotations
 *
 * @author Hardy Ferentschik
 */
public interface JPADotNames {
	public static final DotName ENTITY = DotName.createSimple( Entity.class.getName() );
	public static final DotName HIBERNATE_ENTITY = DotName.createSimple( org.hibernate.annotations.Entity.class.getName() );
	public static final DotName MAPPED_SUPER_CLASS = DotName.createSimple( MappedSuperclass.class.getName() );

	public static final DotName ID = DotName.createSimple( Id.class.getName() );

}


