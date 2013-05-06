/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
 * When applied to a class, indicates that all of its properties should be audited.
 * When applied to a field, indicates that this field should be audited.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Tomasz Bech
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Audited {
	/**
	 * Specifies modification store to use
	 */
	ModificationStore modStore() default ModificationStore.FULL;

	/**
	 * Specifies if the entity that is the target of the relation should be audited or not. If not, then when
	 * reading a historic version an audited entity, the relation will always point to the "current" entity.
	 * This is useful for dictionary-like entities, which don't change and don't need to be audited.
	 */
	RelationTargetAuditMode targetAuditMode() default RelationTargetAuditMode.AUDITED;

	/**
	 * Specifies the superclasses for which properties should be audited, even if the superclasses are not
	 * annotated with {@link Audited}. Causes all properties of the listed classes to be audited, just as if the
	 * classes had {@link Audited} annotation applied on the class level.
	 * <p/>
	 * The scope of this functionality is limited to the class hierarchy of the annotated entity.
	 * <p/>
	 * If a parent type lists any of its parent types using this attribute, all properties in the specified classes
	 * will also be audited.
	 *
	 * @deprecated Use {@code @AuditOverride(forClass=SomeEntity.class)} instead.
	 */
	@Deprecated
	Class[] auditParents() default {};

	/**
	 * Should a modification flag be stored for each property in the annotated class or for the annotated
	 * property. The flag stores information if a property has been changed at a given revision.
	 * This can be used for example in queries.
	 */
	boolean withModifiedFlag() default false;
}
