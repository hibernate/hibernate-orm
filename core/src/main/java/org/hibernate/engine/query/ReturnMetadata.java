package org.hibernate.engine.query;

import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class ReturnMetadata implements Serializable {
	private final String[] returnAliases;
	private final Type[] returnTypes;

	public ReturnMetadata(String[] returnAliases, Type[] returnTypes) {
		this.returnAliases = returnAliases;
		this.returnTypes = returnTypes;
	}

	public String[] getReturnAliases() {
		return returnAliases;
	}

	public Type[] getReturnTypes() {
		return returnTypes;
	}
}
