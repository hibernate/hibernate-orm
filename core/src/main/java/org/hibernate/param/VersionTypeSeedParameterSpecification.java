// $Id: VersionTypeSeedParameterSpecification.java 8513 2005-11-02 18:47:40Z steveebersole $
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.VersionType;
import org.hibernate.type.Type;
import org.hibernate.param.ParameterSpecification;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of VersionTypeSeedParameterSpecification.
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification implements ParameterSpecification {

	private VersionType type;

	public VersionTypeSeedParameterSpecification(VersionType type) {
		this.type = type;
	}

	/**
	 * @see org.hibernate.param.ParameterSpecification#bind
	 */
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
	        throws SQLException {
		type.nullSafeSet( statement, type.seed( session ), position, session );
		return 1;
	}

	public Type getExpectedType() {
		return type;
	}

	public void setExpectedType(Type expectedType) {
		// expected type is intrinsic here...
	}

	public String renderDisplayInfo() {
		return "version-seed, type=" + type;
	}
}
