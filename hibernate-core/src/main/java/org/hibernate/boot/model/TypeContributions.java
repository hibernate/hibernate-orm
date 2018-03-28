/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * Defines the target contributing types, whether via dialects or {@link TypeContributor}
 *
 * @author Steve Ebersole
 */
public interface TypeContributions {
	void contributeType(BasicType type);

	/**
	 * @deprecated (since 5.3) It will be replaced by {@link #contributeType(BasicType)}
	 * but do not move to it before 6.0
	 */
	@Deprecated
	void contributeType(BasicType type, String... keys);

	/**
	 * @deprecated (since 5.3) It will be replaced by {@link #contributeType(BasicType)}
	 * but do not move to it before 6.0
	 */
	void contributeType(UserType type, String... keys);

	/**
	 * @deprecated (since 5.3) It will be replaced by {@link #contributeType(BasicType)}
	 * but do not move to it before 6.0
	 */
	void contributeType(CompositeUserType type, String... keys);

	/*
	 * Add the JavaTypeDescriptor to the
	 * @param descriptor
	 */
	void contributeJavaTypeDescriptor(JavaTypeDescriptor descriptor);

	void contributeSqlTypeDescriptor(SqlTypeDescriptor descriptor);

	TypeConfiguration getTypeConfiguration();
}
