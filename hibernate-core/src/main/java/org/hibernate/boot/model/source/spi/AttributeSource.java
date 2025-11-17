/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.source.internal.hbm.XmlElementMetadata;

/**
 * Contract for sources of persistent attribute descriptions.
 * <p>
 * These values are used to build {@link org.hibernate.mapping.Property} instances.
 *
 * @author Steve Ebersole
 */
public interface AttributeSource extends ToolingHintContextContainer {
	XmlElementMetadata getSourceType();

	/**
	 * Obtain the attribute name.
	 *
	 * @return The attribute name. {@code null} is NOT allowed!
	 */
	String getName();

	/**
	 * Attributes are (coarsely speaking) either singular or plural.
	 *
	 * @return {@code true} indicates the attribute is singular (and therefore castable
	 * to {@link SingularAttributeSource}); {@code false} indicates it is plural (and
	 * therefore castable to {@link PluralAttributeSource}).
	 */
	boolean isSingular();

	/**
	 * This is only useful to log warnings when these deprecated attributes are populated.
	 * It was only useful for DOM4J entity-mode, which was removed a long time ago.
	 *
	 * @return The xml node name
	 */
	String getXmlNodeName();

	AttributePath getAttributePath();
	AttributeRole getAttributeRole();

	/**
	 * Obtain information about the Hibernate type ({@link org.hibernate.type.Type}) for this attribute.
	 *
	 * @return The Hibernate type information
	 */
	HibernateTypeSource getTypeInformation();

	/**
	 * Obtain the name of the property accessor style used to access this attribute.
	 *
	 * @return The property accessor style for this attribute.
	 *
	 * @see org.hibernate.property.access.spi.PropertyAccessStrategy
	 */
	String getPropertyAccessorName();

	/**
	 * If the containing entity is using optimistic locking, should this
	 * attribute participate in that locking?  Meaning, should changes in the
	 * value of this attribute at runtime indicate that the entity is now dirty
	 * in terms of optimistic locking?
	 *
	 * @return {@code true} indicates it should be included; {@code false}, it should not.
	 */
	boolean isIncludedInOptimisticLocking();
}
