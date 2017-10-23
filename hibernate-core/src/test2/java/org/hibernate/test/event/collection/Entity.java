/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection;


/**
 * Created by IntelliJ IDEA.
 * User: gbadner
 * Date: Jan 30, 2008
 * Time: 2:39:37 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Entity {
	Long getId();

	void setId(Long id);
}
