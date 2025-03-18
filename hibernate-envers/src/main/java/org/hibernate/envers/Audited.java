/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

/**
 * When applied to a class, indicates that all of its properties should be audited.
 * When applied to a field, indicates that this field should be audited.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Tomasz Bech
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 * @author Chris Cranford
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Audited {
	/**
	 * Specifies if the entity that is the target of the relation should be audited or not. If not, then when
	 * reading a historic version an audited entity, the relation will always point to the "current" entity.
	 * This is useful for dictionary-like entities, which don't change and don't need to be audited.
	 */
	RelationTargetAuditMode targetAuditMode() default RelationTargetAuditMode.AUDITED;

	/**
	 * Specifies if the entity that is the relation target isn't found, how should the system react.
	 *
	 * The default is to use the behavior configured based on the system property:
	 * {@link org.hibernate.envers.configuration.EnversSettings#GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG}.
	 *
	 * When the configuration property is {@code true}, this is to use the legacy behavior which
	 * implies that the system should throw the {@code EntityNotFoundException} errors unless
	 * the user has explicitly specified the value {@link RelationTargetNotFoundAction#IGNORE}.
	 *
	 * When the configuration property is {@code false}, this is to use the new behavior which
	 * implies that the system should ignore the {@code EntityNotFoundException} errors unless
	 * the user has explicitly specified the value {@link RelationTargetNotFoundAction#ERROR}.
	 */
	@Incubating
	RelationTargetNotFoundAction targetNotFoundAction() default RelationTargetNotFoundAction.DEFAULT;

	/**
	 * Specifies the superclasses for which properties should be audited, even if the superclasses are not
	 * annotated with {@link Audited}. Causes all properties of the listed classes to be audited, just as if the
	 * classes had {@link Audited} annotation applied on the class level.
	 * <p>
	 * The scope of this functionality is limited to the class hierarchy of the annotated entity.
	 * <p>
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

	/**
	 * The column name of the modified field. Analogous to the name attribute of the @{@link jakarta.persistence.Column}
	 * annotation. Ignored if withModifiedFlag is false.
	 */
	String modifiedColumnName() default "";
}
