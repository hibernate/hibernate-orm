/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.type.descriptor.java.internal.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class RevisionTimestampData extends PropertyData {

	private JavaTypeDescriptor javaTypeDescriptor;

	public RevisionTimestampData(String name, String beanName, String accessType, JavaTypeDescriptor javaTypeDescriptor) {
		super( name, beanName, accessType, null );
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public RevisionTimestampData(RevisionTimestampData old, JavaTypeDescriptor javaTypeDescriptor) {
		this( old.getName(), old.getBeanName(), old.getAccessType(), javaTypeDescriptor );
	}

	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public boolean isTimestampDate() {
		// todo (6.0) - this shouldn't ever be the case
		assert( javaTypeDescriptor != null );
		return TemporalJavaDescriptor.class.isInstance( javaTypeDescriptor );
	}

	public boolean isTimestampLocalDateTime() {
		return LocalDateJavaDescriptor.class.isInstance( javaTypeDescriptor );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + javaTypeDescriptor.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		RevisionTimestampData that = (RevisionTimestampData) o;

		if ( !javaTypeDescriptor.equals( that.javaTypeDescriptor ) ) {
			return false;
		}

		return true;
	}
}
