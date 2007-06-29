//$Id: Value.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.Type;

/**
 * A value is anything that is persisted by value, instead of
 * by reference. It is essentially a Hibernate Type, together
 * with zero or more columns. Values are wrapped by things with
 * higher level semantics, for example properties, collections,
 * classes.
 *
 * @author Gavin King
 */
public interface Value extends Serializable {
	public int getColumnSpan();
	public Iterator getColumnIterator();
	public Type getType() throws MappingException;
	public FetchMode getFetchMode();
	public Table getTable();
	public boolean hasFormula();
	public boolean isAlternateUniqueKey();
	public boolean isNullable();
	public boolean[] getColumnUpdateability();
	public boolean[] getColumnInsertability();
	public void createForeignKey() throws MappingException;
	public boolean isSimpleValue();
	public boolean isValid(Mapping mapping) throws MappingException;
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException;
	public Object accept(ValueVisitor visitor);
}