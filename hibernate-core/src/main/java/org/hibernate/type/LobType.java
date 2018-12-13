/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.LobMergeStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * An abstract superclass for Lob types.
 * @author Gail Badner
 */
public abstract class LobType<T> extends AbstractSingleColumnStandardBasicType<T> {

	public LobType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
	}

	protected Object doReplacement(Object original, Object target, SharedSessionContractImplementor session) {
		final LobMergeStrategy lobMergeStrategy =
				session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy();
		if ( !lobMergeStrategy.supportsMerge() ) {
			return target;
		}
		return super.doReplacement( original, target, session );
	}
}
