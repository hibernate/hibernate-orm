//$Id$
package org.hibernate.mapping;

/**
 * Interface allowing to differenciate SubClasses
 * from Classes, JoinedSubClasses and UnionSubClasses
 * The first one has not its own table while the others have
 * 
 * @author Emmanuel Bernard
 */
public interface TableOwner {
	void setTable(Table table);
}
