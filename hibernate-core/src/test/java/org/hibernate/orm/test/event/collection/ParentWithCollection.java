/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;
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
