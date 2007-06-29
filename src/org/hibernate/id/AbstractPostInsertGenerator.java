//$Id$
package org.hibernate.id;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public abstract class AbstractPostInsertGenerator implements PostInsertIdentifierGenerator{
	public Serializable generate(SessionImplementor s, Object obj) {
		return IdentifierGeneratorFactory.POST_INSERT_INDICATOR;
	}
}
