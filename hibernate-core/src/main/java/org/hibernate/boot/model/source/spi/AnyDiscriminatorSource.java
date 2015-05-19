/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

import org.hibernate.boot.model.naming.ImplicitAnyDiscriminatorColumnNameSource;

/**
 * Source information about the discriminator for an ANY mapping
 *
 * @author Steve Ebersole
 */
public interface AnyDiscriminatorSource extends ImplicitAnyDiscriminatorColumnNameSource {
	HibernateTypeSource getTypeSource();
	RelationalValueSource getRelationalValueSource();
	Map<String,String> getValueMappings();
}
