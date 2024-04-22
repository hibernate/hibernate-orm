/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.AnnotationTarget;

import jakarta.persistence.AccessType;

/**
 * Possible class-level {@linkplain jakarta.persistence.AccessType} values.
 *
 * @author Steve Ebersole
 */
public enum ClassAttributeAccessType {
	/**
	 * The class explicitly defined field access via {@linkplain jakarta.persistence.Access}
	 */
	EXPLICIT_FIELD(true, AccessType.FIELD),

	/**
	 * The class explicitly defined property access via {@linkplain jakarta.persistence.Access}
	 */
	EXPLICIT_PROPERTY(true, AccessType.PROPERTY),

	/**
	 * The class is using field access from its hierarchy's "default access type". See section
	 * <i>2.3.1 Default Access Type</i> for more details, but roughly stated this is deduced based
	 * on the placement of {@linkplain jakarta.persistence.Id @Id} /
	 * {@linkplain jakarta.persistence.EmbeddedId @EmbeddedId} annotations
	 */
	IMPLICIT_FIELD(false, AccessType.FIELD),

	/**
	 * The class is using property access from its hierarchy's "default access type". See section
	 * <i>2.3.1 Default Access Type</i> for more details, but roughly stated this is deduced based
	 * on the placement of {@linkplain jakarta.persistence.Id @Id} /
	 * {@linkplain jakarta.persistence.EmbeddedId @EmbeddedId} annotations
	 */
	IMPLICIT_PROPERTY(false, AccessType.PROPERTY);

	private final boolean explicit;
	private final AccessType jpaAccessType;
	private final AnnotationTarget.Kind targetKind;

	ClassAttributeAccessType(boolean explicit, AccessType jpaAccessType) {
		this.explicit = explicit;
		this.jpaAccessType = jpaAccessType;
		this.targetKind = jpaAccessType == AccessType.FIELD ? AnnotationTarget.Kind.FIELD : AnnotationTarget.Kind.METHOD;
	}

	/**
	 * Whether the access-type was explicitly specified
	 */
	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * The corresponding {@linkplain jakarta.persistence.AccessType}
	 */
	public AccessType getJpaAccessType() {
		return jpaAccessType;
	}

	/**
	 * The annotation target kind which correlates to the given {@linkplain #getJpaAccessType() access type}
	 */
	public AnnotationTarget.Kind getTargetKind() {
		return targetKind;
	}
}
