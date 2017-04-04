/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are a one-to-many association
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceOneToMany extends PluralAttributeElementSourceAssociation {
	public String getReferencedEntityName();

	public boolean isIgnoreNotFound();

	public String getXmlNodeName();
}
