package org.hibernate.id.insert;

import org.hibernate.engine.SessionImplementor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Responsible for handling delegation relating to variants in how
 * insert-generated-identifier generator strategies dictate processing:<ul>
 * <li>building the sql insert statement
 * <li>determination of the generated identifier value
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface InsertGeneratedIdentifierDelegate {

	/**
	 * Build a {@link org.hibernate.sql.Insert} specific to the delegate's mode
	 * of handling generated key values.
	 *
	 * @return The insert object.
	 */
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert();

	/**
	 * Perform the indicated insert SQL statement and determine the identifier value
	 * generated.
	 *
	 * @param insertSQL
	 * @param session
	 * @param binder
	 * @return The generated identifier value.
	 */
	public Serializable performInsert(String insertSQL, SessionImplementor session, Binder binder);

}
