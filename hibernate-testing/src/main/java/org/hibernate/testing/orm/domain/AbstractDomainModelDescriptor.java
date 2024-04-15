/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain;

import org.hibernate.boot.MetadataSources;

/**
 * Convenience base class test domain models based on annotated classes
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDomainModelDescriptor implements DomainModelDescriptor {
	private final Class[] annotatedClasses;

	protected AbstractDomainModelDescriptor(Class... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return annotatedClasses;
	}

	@Override
	public void applyDomainModel(MetadataSources sources) {
		for ( Class annotatedClass : annotatedClasses ) {
			sources.addAnnotatedClass( annotatedClass );
		}
	}
}
