/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.BasicValueBinder;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Sharath Reddy
 */
public class SetBasicValueTypeSecondPass implements SecondPass {
	private final BasicValueBinder binder;

	public SetBasicValueTypeSecondPass(BasicValueBinder val) {
		binder = val;
	}

	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		binder.fillSimpleValue();
	}
}
