/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicTypeAdapter<J> implements org.hibernate.type.spi.BasicType<J> {
	private final BasicType customType;
	private final String name;

	private final BasicJavaDescriptor<J> jtd;

	private final SqlExpressableType sqlExpressableType;


	private BasicTypeAdapter(
			BasicType customType,
			String name,
			BasicJavaDescriptor<J> jtd,
			SqlTypeDescriptor std) {
		this.customType = customType;
		this.name = name;
		this.jtd = jtd;
		this.sqlExpressableType = new BasicSqlExpressableTypeAdapter( jtd, std, customType );
	}

	@SuppressWarnings("unchecked")
	public BasicTypeAdapter(BasicType customType, String name) {
		this(
				customType,
				name,
				customType.getJavaTypeDescriptor(),
				customType.getSqlTypeDescriptor()
		);
	}

	public BasicTypeAdapter(
			BasicType customType,
			String name,
			BasicJavaDescriptor explicitJtd,
			SqlTypeDescriptor explicitStd,
			ResolutionContext resolutionContext) {
		this(
				customType,
				name,
				determineJavaTypeDescriptor( customType, explicitJtd ),
				explicitStd != null
						? explicitStd
						: customType.getSqlTypeDescriptor()
		);
	}

	@SuppressWarnings("unchecked")
	private static <J> BasicJavaDescriptor<J> determineJavaTypeDescriptor(BasicType customType, BasicJavaDescriptor explicitJtd) {
		return explicitJtd != null
				? explicitJtd
				: customType.getJavaTypeDescriptor();
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return jtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return sqlExpressableType;
	}
}
