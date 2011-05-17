/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml;

import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 */
public interface PseudoJpaDotNames {
	public static final DotName DEFAULT_ACCESS = DotName.createSimple( "default.access" );
	public static final DotName DEFAULT_DELIMITED_IDENTIFIERS = DotName.createSimple( "default.delimited.identifiers" );
	public static final DotName DEFAULT_ENTITY_LISTENERS = DotName.createSimple( "default.entity.listeners" );
	public static final DotName DEFAULT_POST_LOAD = DotName.createSimple( "default.entity.listener.post.load" );
	public static final DotName DEFAULT_POST_PERSIST = DotName.createSimple( "default.entity.listener.post.persist" );
	public static final DotName DEFAULT_POST_REMOVE = DotName.createSimple( "default.entity.listener.post.remove" );
	public static final DotName DEFAULT_POST_UPDATE = DotName.createSimple( "default.entity.listener.post.update" );
	public static final DotName DEFAULT_PRE_PERSIST = DotName.createSimple( "default.entity.listener.pre.persist" );
	public static final DotName DEFAULT_PRE_REMOVE = DotName.createSimple( "default.entity.listener.pre.remove" );
	public static final DotName DEFAULT_PRE_UPDATE = DotName.createSimple( "default.entity.listener.pre.update" );
}
