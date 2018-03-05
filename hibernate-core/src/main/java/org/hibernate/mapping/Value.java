/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
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
	public Iterator<Selectable> getColumnIterator();
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
	public boolean isSame(Value other);

	ServiceRegistry getServiceRegistry();
}
