/**
 * Test package for metatata facilities
 * It contains an example of filter metadata
 */
@TypeDefs(
		{
		@TypeDef(
				name = "wrapped_char_text",
				typeClass = org.hibernate.test.annotations.lob.CharacterArrayTextType.class
				),
		@TypeDef(
				name = "char_text",
				typeClass = org.hibernate.test.annotations.lob.PrimitiveCharacterArrayTextType.class
		),
		@TypeDef(
				name = "wrapped_image",
				typeClass = org.hibernate.test.annotations.lob.WrappedImageType.class
		),
		@TypeDef(
				name = "serializable_image",
				typeClass = org.hibernate.test.annotations.lob.SerializableToImageType.class
		)
		}
)
package org.hibernate.test.annotations.lob;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

