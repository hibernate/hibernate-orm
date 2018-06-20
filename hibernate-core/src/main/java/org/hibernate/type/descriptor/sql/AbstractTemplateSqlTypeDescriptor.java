/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import org.hibernate.type.descriptor.JdbcValueMapper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcValueMapperImpl;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractTemplateSqlTypeDescriptor extends AbstractSqlTypeDescriptor {
	@Override
	public <X> JdbcValueMapper<X> getJdbcValueMapper(BasicJavaDescriptor<X> javaTypeDescriptor) {
		return determineValueMapper(
				javaTypeDescriptor,
				jtd -> {
					final ValueBinder<X> binder = createBinder( javaTypeDescriptor );
					final ValueExtractor<X> extractor = createExtractor( javaTypeDescriptor );

					return new JdbcValueMapperImpl<X>( javaTypeDescriptor, this, extractor, binder );
				}
		);
	}

	protected abstract <X> ValueBinder<X> createBinder(BasicJavaDescriptor<X> javaTypeDescriptor);

	protected abstract <X> ValueExtractor<X> createExtractor(BasicJavaDescriptor<X> javaTypeDescriptor);
}

