/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.SimpleValueBinder;

/**
 * @author Sharath Reddy
 */
public class SetSimpleValueTypeSecondPass implements SecondPass {

	SimpleValueBinder binder;

	public SetSimpleValueTypeSecondPass(SimpleValueBinder val) {
		binder = val;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		binder.fillSimpleValue();
	}
}
