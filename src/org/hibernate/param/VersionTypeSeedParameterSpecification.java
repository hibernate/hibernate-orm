// $Id$
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.VersionType;
import org.hibernate.type.Type;
import org.hibernate.param.ParameterSpecification;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter bind specification used for optimisitc lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification implements ParameterSpecification {
	private VersionType type;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param type The version type.
	 */
	public VersionTypeSeedParameterSpecification(VersionType type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
	        throws SQLException {
		type.nullSafeSet( statement, type.seed( session ), position, session );
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getExpectedType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpectedType(Type expectedType) {
		// expected type is intrinsic here...
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "version-seed, type=" + type;
	}
}
