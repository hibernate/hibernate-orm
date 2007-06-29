// $Id: AbstractAuxiliaryDatabaseObject.java 7801 2005-08-10 12:16:55Z steveebersole $
package org.hibernate.mapping;

import java.util.HashSet;

import org.hibernate.dialect.Dialect;

/**
 * Convenience base class for {@link AuxiliaryDatabaseObject}s.
 * <p/>
 * This implementation performs dialect scoping checks strictly based on
 * dialect name comparisons.  Custom implementations might want to do
 * instanceof-type checks.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAuxiliaryDatabaseObject implements AuxiliaryDatabaseObject {

	private final HashSet dialectScopes;

	protected AbstractAuxiliaryDatabaseObject() {
		this.dialectScopes = new HashSet();
	}

	protected AbstractAuxiliaryDatabaseObject(HashSet dialectScopes) {
		this.dialectScopes = dialectScopes;
	}

	public void addDialectScope(String dialectName) {
		dialectScopes.add( dialectName );
	}

	public HashSet getDialectScopes() {
		return dialectScopes;
	}

	public boolean appliesToDialect(Dialect dialect) {
		// empty means no scoping
		return dialectScopes.isEmpty() || dialectScopes.contains( dialect.getClass().getName() );
	}
}
