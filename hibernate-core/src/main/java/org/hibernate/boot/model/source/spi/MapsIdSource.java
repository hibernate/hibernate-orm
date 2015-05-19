/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes a relationship annotated with {@link javax.persistence.MapsId}
 * 
 * @author Steve Ebersole
 */
public interface MapsIdSource {
	/**
	 * Obtain the {@link javax.persistence.MapsId#value()} naming the attribute
	 * within the EmbeddedId mapped by this relationship.
	 * 
	 * @return The corresponding id attribute name.
	 */
	public String getMappedIdAttributeName();

	/**
	 * The attribute source information
	 * 
	 * @return The association attribute information
	 */
	public SingularAttributeSourceToOne getAssociationAttributeSource();
}
