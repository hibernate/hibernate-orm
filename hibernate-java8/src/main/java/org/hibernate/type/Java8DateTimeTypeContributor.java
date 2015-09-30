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
import org.hibernate.type.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;

/**
 * TypeContributor for adding Java8 Date/Time specific Type implementations
 *
 * @author Steve Ebersole
 */
public class Java8DateTimeTypeContributor implements TypeContributor {
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// register the Java type descriptors
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( DurationJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( InstantJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( LocalDateJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( LocalDateTimeJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( OffsetDateTimeJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( OffsetTimeJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( ZonedDateTimeJavaDescriptor.INSTANCE );

		// register the Hibernate type mappings
		typeContributions.contributeType( DurationType.INSTANCE );
		typeContributions.contributeType( InstantType.INSTANCE );
		typeContributions.contributeType( LocalDateTimeType.INSTANCE );
		typeContributions.contributeType( LocalDateType.INSTANCE );
		typeContributions.contributeType( LocalTimeType.INSTANCE );
		typeContributions.contributeType( OffsetDateTimeType.INSTANCE );
		typeContributions.contributeType( OffsetTimeType.INSTANCE );
		typeContributions.contributeType( ZonedDateTimeType.INSTANCE );
	}
}
