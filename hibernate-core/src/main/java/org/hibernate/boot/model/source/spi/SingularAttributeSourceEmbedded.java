/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Represents the binding source for a singular attribute that is "embedded"
 * or "composite".
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSourceEmbedded extends SingularAttributeSource, EmbeddableSourceContributor {
}
