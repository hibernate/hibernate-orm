/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes source information about the key of a persistent map.  At high
 * level this broken down further into 2 categories:<ul>
 *     <li>{@link PluralAttributeMapKeySourceEntityAttribute}</li>
 *     <li>
 *         <ul>
 *             <li>{@link PluralAttributeMapKeySourceBasic}</li>
 *             <li>{@link PluralAttributeMapKeySourceEmbedded}</li>
 *             <li>{@link PluralAttributeMapKeyManyToManySource}</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p/>
 * {@link PluralAttributeMapKeySourceEntityAttribute} is only relevant from
 * annotations when using {@link javax.persistence.MapKey}.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMapKeySource extends PluralAttributeIndexSource {
	public static enum Nature {
		BASIC,
		EMBEDDED,
		MANY_TO_MANY,
		ANY
	}

	public Nature getMapKeyNature();

	/**
	 * Is this plural attribute index source for an attribute of the referenced entity
	 * (relevant only for one-to-many and many-to-many associations)?
	 *
	 * If this method returns {@code true}, then this object can safely
	 * be cast to {@link PluralAttributeMapKeySourceEntityAttribute}.
	 *
	 * @return true, if this plural attribute index source for an attribute of the referenced
	 * entity; false, otherwise.
	 */
	public boolean isReferencedEntityAttribute();
}
