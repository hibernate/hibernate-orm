/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity to be created whenever a new revision is generated. The revisions entity must have
 * an integer-valued unique property (preferably the primary id) annotated with {@link RevisionNumber}
 * and a long-valued property annotated with {@link RevisionTimestamp}. The {@link DefaultRevisionEntity}
 * already has those two fields, so you may extend it, but you may also write your own revision entity
 * from scratch.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RevisionEntity {
	/**
	 * The optional listener that will be used to fill in the custom revision entity.
	 * May also be specified using the {@code org.hibernate.envers.revision_listener} configuration property.
	 */
	Class<? extends RevisionListener> value() default RevisionListener.class;
}
