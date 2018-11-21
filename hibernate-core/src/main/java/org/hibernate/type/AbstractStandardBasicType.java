/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Convenience base class for {@link BasicType} implementations
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 *
 * @deprecated Ported simply to support {@link BasicType} as closely as
 * possible to its legacy definition.  Users were often directed to use this
 * as a base for custom "types"
 *
 * @see BasicType
 */
@Deprecated
public abstract class AbstractStandardBasicType<T>
		implements BasicType, ProcedureParameterExtractionAware<T>, ProcedureParameterNamedBinder {

	private BasicJavaDescriptor<T> javaTypeDescriptor;
	private SqlTypeDescriptor sqlTypeDescriptor;

	public AbstractStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.javaTypeDescriptor = (BasicJavaDescriptor<T>) javaTypeDescriptor;
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	@Override
	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaTypeDescriptor.getJavaType().getName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}

	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public final SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}


}
