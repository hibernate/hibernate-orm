/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.interfaces.hbm.allAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern&aacute;n Chanfreau
 */
@Audited
public interface SimpleInterface {

	long getId();

	void setId(long id);

	String getData();

	void setData(String data);

}
