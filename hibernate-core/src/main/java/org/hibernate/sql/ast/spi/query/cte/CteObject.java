package org.hibernate.sql.ast.spi.query.cte;

/**
 * An object that is part of a WITH clause.
 *
 * @author Christian Beikov
 */
public interface CteObject {

	String getName();

}
