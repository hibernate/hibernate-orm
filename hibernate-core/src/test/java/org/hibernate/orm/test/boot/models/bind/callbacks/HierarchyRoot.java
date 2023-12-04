/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PostLoad;

/**
 * @author Steve Ebersole
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(Listener2.class)
public class HierarchyRoot extends HierarchySuper {
	@PostLoad
	public void wasLoaded() {}
}
