/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper;

/**
 * Contract for {@link PropertyMapper} implementations to expose whether they contain any property
 * that uses {@link org.hibernate.envers.internal.entities.PropertyData#isUsingModifiedFlag()}.
 *
 * @author Chris Cranford
 */
public interface ModifiedFlagMapperSupport {
	/**
	 * Returns whether the associated {@link PropertyMapper} has any properties that use
	 * the {@code witModifiedFlag} feature.
	 *
	 * @return {@code true} if a property uses {@code withModifiedFlag}, otherwise {@code false}.
	 */
	boolean hasPropertiesWithModifiedFlag();
}
