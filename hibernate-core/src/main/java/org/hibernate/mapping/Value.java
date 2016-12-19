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
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.Type;

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
	int getColumnSpan();
	Iterator<Selectable> getColumnIterator();
	Type getType() throws MappingException;
	FetchMode getFetchMode();
	Table getTable();
	boolean hasFormula();
	boolean isAlternateUniqueKey();
	boolean isNullable();
	boolean[] getColumnUpdateability();
	boolean[] getColumnInsertability();
	void createForeignKey() throws MappingException;
	boolean isSimpleValue();
	boolean isValid(Mapping mapping) throws MappingException;
	void setTypeUsingReflection(String className, String propertyName) throws MappingException;
	Object accept(ValueVisitor visitor);

	ServiceRegistry getServiceRegistry();
}
