/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
