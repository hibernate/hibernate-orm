/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.source.internal.hbm.XmlElementMetadata;

/**
 * Contract for sources of persistent attribute descriptions.
 * <p/>
 * These values are used to build {@link org.hibernate.mapping.Property} instances.
 *
 * @author Steve Ebersole
 */
public interface AttributeSource extends ToolingHintContextContainer {
	public XmlElementMetadata getSourceType();

	/**
	 * Obtain the attribute name.
	 *
	 * @return The attribute name. {@code null} is NOT allowed!
	 */
	public String getName();

	/**
	 * Attributes are (coarsely speaking) either singular or plural.
	 *
	 * @return {@code true} indicates the attribute is singular (and therefore castable
	 * to {@link SingularAttributeSource}); {@code false} indicates it is plural (and
	 * therefore castable to {@link PluralAttributeSource}).
	 */
	public boolean isSingular();

	/**
	 * Ugh.  This is the deprecated DOM4J entity-mode feature
	 *
	 * @return The xml node name
	 */
	public String getXmlNodeName();

	public AttributePath getAttributePath();
	public AttributeRole getAttributeRole();

	/**
	 * Obtain information about the Hibernate type ({@link org.hibernate.type.Type}) for this attribute.
	 *
	 * @return The Hibernate type information
	 */
	public HibernateTypeSource getTypeInformation();

	/**
	 * Obtain the name of the property accessor style used to access this attribute.
	 *
	 * @return The property accessor style for this attribute.
	 *
	 * @see org.hibernate.property.access.spi.PropertyAccessStrategy
	 */
	public String getPropertyAccessorName();

	/**
	 * If the containing entity is using optimistic locking, should this
	 * attribute participate in that locking?  Meaning, should changes in the
	 * value of this attribute at runtime indicate that the entity is now dirty
	 * in terms of optimistic locking?
	 *
	 * @return {@code true} indicates it should be included; {@code false}, it should not.
	 */
	public boolean isIncludedInOptimisticLocking();
}

