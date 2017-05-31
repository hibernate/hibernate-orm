/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test.client;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;


/**
 * @author Brett Meyer
 */
public class TestTypeContributor implements TypeContributor {

	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeJavaTypeDescriptor( TestTypeJavaDescriptor.INSTANCE );
	}

	public static class TestType {

	}

	public static class TestTypeJavaDescriptor extends AbstractBasicJavaDescriptor<TestType> {
		public static TestTypeJavaDescriptor INSTANCE = new TestTypeJavaDescriptor();

		public TestTypeJavaDescriptor() {
			super( TestType.class );
		}

		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
			return null;
		}

		@Override
		public <X> X unwrap(TestType value, Class<X> type, WrapperOptions options) {
			return null;
		}

		@Override
		public <X> TestType wrap(X value, WrapperOptions options) {
			return null;
		}
	}
}
