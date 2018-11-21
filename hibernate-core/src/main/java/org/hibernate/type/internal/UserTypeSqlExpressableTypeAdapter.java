/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeSqlExpressableTypeAdapter implements SqlExpressableType {
	private final BasicJavaDescriptor domainJavaType;
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private final UserType userType;

	private final UserTypeBinderAdapter binder;
	private final UserTypeExtractorAdaptor extractor;

	public UserTypeSqlExpressableTypeAdapter(
			BasicJavaDescriptor domainJavaType,
			SqlTypeDescriptor sqlTypeDescriptor,
			UserType userType) {

		this.domainJavaType = domainJavaType;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.userType = userType;

		this.binder = new UserTypeBinderAdapter( userType );
		this.extractor = new UserTypeExtractorAdaptor( userType );
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return domainJavaType;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public JdbcValueExtractor getJdbcValueExtractor() {
		return extractor;
	}

	@Override
	public JdbcValueBinder getJdbcValueBinder() {
		return binder;
	}

	@Override
	public String toString() {
		return "UserTypeSqlExpressableTypeAdapter(" + userType + ')';
	}
}
