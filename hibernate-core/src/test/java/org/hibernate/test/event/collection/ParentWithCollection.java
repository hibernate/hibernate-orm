/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection;
import java.util.Collection;

/**
 *
 * @author Gail Badner
 */
public interface ParentWithCollection extends Entity {
	void newChildren(Collection collection);

	Child createChild(String name);

	Long getId();

	void setId(Long id);

	String getName();

	void setName(String name);

	Collection getChildren();

	void setChildren(Collection children);

	Child addChild(String name);

	void addChild(Child child);

	void addAllChildren(Collection children);

	void removeChild(Child child);

	void removeAllChildren(Collection children);

	void clearChildren();
}
