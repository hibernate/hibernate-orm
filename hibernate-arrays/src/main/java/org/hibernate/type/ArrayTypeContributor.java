/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;

/**
 * TypeContributor for adding single-dimensional arrays
 *
 * @author Jordan Gigov
 */
public class ArrayTypeContributor implements TypeContributor {

	@Override
	public void contribute( TypeContributions typeContributions, ServiceRegistry serviceRegistry ) {

		// Do we really need all these types?
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BOOLEAN.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.NUMERIC_BOOLEAN.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.TRUE_FALSE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.YES_NO.getJavaTypeDescriptor() );
//		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BYTE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.SHORT.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.INTEGER.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.LONG.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.FLOAT.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.DOUBLE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BIG_INTEGER.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BIG_DECIMAL.getJavaTypeDescriptor() );
//		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CHARACTER.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.STRING.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.URL.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.TIME.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.DATE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.TIMESTAMP.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CALENDAR.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CALENDAR_DATE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CLASS.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.LOCALE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CURRENCY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.TIMEZONE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.UUID_BINARY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.UUID_CHAR.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BINARY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.WRAPPER_BINARY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.IMAGE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.BLOB.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.MATERIALIZED_BLOB.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CHAR_ARRAY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CHARACTER_ARRAY.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.TEXT.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.CLOB.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.MATERIALIZED_CLOB.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.SERIALIZABLE.getJavaTypeDescriptor() );

		// Java 8 time classes
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.INSTANT.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.DURATION.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.LOCAL_DATE_TIME.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.LOCAL_DATE.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.LOCAL_TIME.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.ZONED_DATE_TIME.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.OFFSET_DATE_TIME.getJavaTypeDescriptor() );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ArrayTypes.OFFSET_TIME.getJavaTypeDescriptor() );

		// register the Hibernate type mappings
		typeContributions.contributeType( ArrayTypes.BOOLEAN );
		typeContributions.contributeType( ArrayTypes.NUMERIC_BOOLEAN );
		typeContributions.contributeType( ArrayTypes.TRUE_FALSE );
		typeContributions.contributeType( ArrayTypes.YES_NO );
//		typeContributions.contributeType( ArrayTypes.BYTE );
		typeContributions.contributeType( ArrayTypes.SHORT );
		typeContributions.contributeType( ArrayTypes.INTEGER );
		typeContributions.contributeType( ArrayTypes.LONG );
		typeContributions.contributeType( ArrayTypes.FLOAT );
		typeContributions.contributeType( ArrayTypes.DOUBLE );
		typeContributions.contributeType( ArrayTypes.BIG_INTEGER );
		typeContributions.contributeType( ArrayTypes.BIG_DECIMAL );
//		typeContributions.contributeType( ArrayTypes.CHARACTER );
		typeContributions.contributeType( ArrayTypes.STRING );
		typeContributions.contributeType( ArrayTypes.URL );
		typeContributions.contributeType( ArrayTypes.TIME );
		typeContributions.contributeType( ArrayTypes.DATE );
		typeContributions.contributeType( ArrayTypes.TIMESTAMP );
		typeContributions.contributeType( ArrayTypes.CALENDAR );
		typeContributions.contributeType( ArrayTypes.CALENDAR_DATE );
		typeContributions.contributeType( ArrayTypes.CLASS );
		typeContributions.contributeType( ArrayTypes.LOCALE );
		typeContributions.contributeType( ArrayTypes.CURRENCY );
		typeContributions.contributeType( ArrayTypes.TIMEZONE );
		typeContributions.contributeType( ArrayTypes.UUID_BINARY );
		typeContributions.contributeType( ArrayTypes.UUID_CHAR );
		typeContributions.contributeType( ArrayTypes.BINARY );
		typeContributions.contributeType( ArrayTypes.WRAPPER_BINARY );
		typeContributions.contributeType( ArrayTypes.IMAGE );
		typeContributions.contributeType( ArrayTypes.BLOB );
		typeContributions.contributeType( ArrayTypes.MATERIALIZED_BLOB );
		typeContributions.contributeType( ArrayTypes.CHAR_ARRAY );
		typeContributions.contributeType( ArrayTypes.CHARACTER_ARRAY );
		typeContributions.contributeType( ArrayTypes.TEXT );
		typeContributions.contributeType( ArrayTypes.CLOB );
		typeContributions.contributeType( ArrayTypes.MATERIALIZED_CLOB );
		typeContributions.contributeType( ArrayTypes.SERIALIZABLE );

		// Java 8 time classes
		typeContributions.contributeType( ArrayTypes.INSTANT );
		typeContributions.contributeType( ArrayTypes.DURATION );
		typeContributions.contributeType( ArrayTypes.LOCAL_DATE_TIME );
		typeContributions.contributeType( ArrayTypes.LOCAL_DATE );
		typeContributions.contributeType( ArrayTypes.LOCAL_TIME );
		typeContributions.contributeType( ArrayTypes.ZONED_DATE_TIME );
		typeContributions.contributeType( ArrayTypes.OFFSET_DATE_TIME );
		typeContributions.contributeType( ArrayTypes.OFFSET_TIME );

	}
}
