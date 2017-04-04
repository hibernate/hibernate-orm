/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
