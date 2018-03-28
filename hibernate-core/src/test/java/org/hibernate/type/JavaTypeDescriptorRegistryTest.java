/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class JavaTypeDescriptorRegistryTest {

	@Test
	public void testGetJavaTypeDescriptorRegistry(){
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		JavaTypeDescriptor<String> descriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
				.getDescriptor( String.class );

		assertThat(descriptor, instanceOf(StringTypeDescriptor.class));
	}

	@Test
	public void testRegisterJavaTypeDescriptorRegistry(){
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		typeConfiguration.getJavaTypeDescriptorRegistry().addDescriptor( new CustomJavaTypeDescriptor() );
		JavaTypeDescriptor descriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
				.getDescriptor( CustomType.class );

		assertThat(descriptor, instanceOf(CustomJavaTypeDescriptor.class));
	}

	@Test
	public void testAddDirectlyToJavaTypeDescriptorRegistry(){
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( new CustomJavaTypeDescriptor()  );
		JavaTypeDescriptor descriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
				.getDescriptor( CustomType.class );

		assertThat(descriptor, instanceOf(CustomJavaTypeDescriptor.class));
	}

	public class CustomType {}

	public class CustomJavaTypeDescriptor implements JavaTypeDescriptor{
		@Override
		public Class getJavaTypeClass() {
			return CustomType.class;
		}

		@Override
		public MutabilityPlan getMutabilityPlan() {
			return null;
		}

		@Override
		public Comparator getComparator() {
			return null;
		}

		@Override
		public int extractHashCode(Object value) {
			return 0;
		}

		@Override
		public boolean areEqual(Object one, Object another) {
			return false;
		}

		@Override
		public String extractLoggableRepresentation(Object value) {
			return null;
		}

		@Override
		public String toString(Object value) {
			return null;
		}

		@Override
		public Object fromString(String string) {
			return null;
		}

		@Override
		public Object wrap(Object value, WrapperOptions options) {
			return null;
		}

		@Override
		public Object unwrap(Object value, Class type, WrapperOptions options) {
			return null;
		}
	}
}
