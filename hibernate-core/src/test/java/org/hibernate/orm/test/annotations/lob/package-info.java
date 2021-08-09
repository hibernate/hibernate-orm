/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Test package for metatata facilities
 * It contains an example of filter metadata
 */
@TypeDef(
		name = "wrapped_char_text",
		typeClass = CharacterArrayTextType.class
		)
@TypeDef(
		name = "char_text",
		typeClass = PrimitiveCharacterArrayTextType.class
)
@TypeDef(
		name = "wrapped_image",
		typeClass = WrappedImageType.class
)
@TypeDef(
		name = "serializable_image",
		typeClass = SerializableToImageType.class
)
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.annotations.TypeDef;

