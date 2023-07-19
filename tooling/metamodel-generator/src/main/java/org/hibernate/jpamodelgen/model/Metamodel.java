/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.model;

import org.hibernate.jpamodelgen.Context;

import java.util.List;
import javax.lang.model.element.Element;

/**
 * @author Hardy Ferentschik
 */
public interface Metamodel extends ImportContext {
	String getSimpleName();

	String getQualifiedName();

	String getPackageName();

	List<MetaAttribute> getMembers();

	String generateImports();

	String importType(String fqcn);

	String staticImport(String fqcn, String member);

	Element getElement();

	boolean isMetaComplete();

	Context getContext();

	boolean isImplementation();

	boolean isInjectable();
}
