/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.spi;

import org.hibernate.type.descriptor.JdbcValueMapper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class JdbcValueMapperImpl<X> implements JdbcValueMapper<X> {
	private final ValueExtractor<X> extractor;
	private final ValueBinder<X> binder;
	private final BasicJavaDescriptor<X> javaTypeDescriptor;
	private final SqlTypeDescriptor sqlTypeDescriptor;

	public JdbcValueMapperImpl(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			ValueExtractor<X> extractor,
			ValueBinder<X> binder) {
		this.extractor = extractor;
		this.binder = binder;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	@Override
	public ValueExtractor<X> getValueExtractor() {
		return this.extractor;
	}

	@Override
	public ValueBinder<X> getValueBinder() {
		return this.binder;
	}

	@Override
	public BasicJavaDescriptor<X> getJavaTypeDescriptor() {
		return this.javaTypeDescriptor;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return this.sqlTypeDescriptor;
	}
}

