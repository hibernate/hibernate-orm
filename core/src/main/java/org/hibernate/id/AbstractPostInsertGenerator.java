//$Id: AbstractPostInsertGenerator.java 9681 2006-03-24 18:10:04Z steve.ebersole@jboss.com $
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
