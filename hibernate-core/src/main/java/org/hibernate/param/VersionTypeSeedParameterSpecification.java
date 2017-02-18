/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.param;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.Type;

/**
 * Parameter bind specification used for optimisitc lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification implements ParameterSpecification {
	private final Type type;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param type The version type.
	 */
	public VersionTypeSeedParameterSpecification(Type type) {
		this.type = type;
	}

	@Override
	public int bind(
			PreparedStatement statement,
			QueryParameters qp,
			SharedSessionContractImplementor session,
			int position) throws SQLException {
		type.nullSafeSet( statement, ( (BasicType) type ).getVersionSupport().seed( session ), position, session );
		return 1;
	}

	@Override
	public Type getExpectedType() {
		return type;
	}

	@Override
	public void setExpectedType(Type expectedType) {
		// expected type is intrinsic here...
	}

	@Override
	public String renderDisplayInfo() {
		return "version-seed, type=" + type;
	}
}
