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
import org.hibernate.type.descriptor.java.IntegerArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.LocalDateArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.LongArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.StringArrayJavaDescriptor;

/**
 * TypeContributor for adding single-dimensional arrays
 *
 * @author Steve Ebersole
 */
public class ArrayTypeContributor implements TypeContributor {
	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		// register the Java type descriptors
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( IntegerArrayJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( LocalDateArrayJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( LongArrayJavaDescriptor.INSTANCE );
		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( StringArrayJavaDescriptor.INSTANCE );

		// register the Hibernate type mappings
		typeContributions.contributeType( IntegerArrayType.INSTANCE );
		typeContributions.contributeType( LocalDateArrayType.INSTANCE );
		typeContributions.contributeType( LongArrayType.INSTANCE );
		typeContributions.contributeType( StringArrayType.INSTANCE );
	}
}
