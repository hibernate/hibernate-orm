/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * Descriptor for the mapping of a basic value as part of the application domain
 * model.  Acts as a composite of everything needed to read/write basic values
 * including any conversions and specialized mutability-plans
 *
 * @author Steve Ebersole
 */
public interface BasicValueMapper<J> extends BasicValuedExpressableType<J> {
	/**
	 * The JTD for the value as part of the domain model
	 */
	BasicJavaDescriptor<J> getDomainJavaDescriptor();

	/**
	 * The BasicJavaDescriptor / SqlTypeDescriptor combo, along
	 * with a binder and an extractor that defines how Hibernate
	 * handles these values at the JDBC level.
	 */
	SqlExpressableType getSqlExpressableType();

	/**
	 * Converter, if any, to convert values between the
	 * domain and relational JavaTypeDescriptor representations
	 */
	BasicValueConverter getValueConverter();

	/**
	 * The resolved MutabilityPlan
	 */
	MutabilityPlan<J> getMutabilityPlan();

	@Override
	default BasicJavaDescriptor getJavaTypeDescriptor() {
		return getDomainJavaDescriptor();
	}

	@Override
	default Class getJavaType() {
		return getDomainJavaDescriptor().getJavaType();
	}
}
