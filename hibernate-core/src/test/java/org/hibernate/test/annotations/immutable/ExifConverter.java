package org.hibernate.test.annotations.immutable;

import java.util.Collections;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 *
 * @author soldierkam
 */
@Converter(autoApply=true)
public class ExifConverter implements AttributeConverter<String, Exif> {

	@Override
	public Exif convertToDatabaseColumn(String attribute) {
		return new Exif( Collections.singletonMap( "fakeAttr", attribute ) );
	}

	@Override
	public String convertToEntityAttribute(Exif dbData) {
		return dbData.getAttributes().get( "fakeAttr" );
	}

}
