/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.instrument.cases;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface Executable {
	public void prepare();
	public void execute() throws Exception;
	public void complete();
	public Class[] getAnnotatedClasses();
}
