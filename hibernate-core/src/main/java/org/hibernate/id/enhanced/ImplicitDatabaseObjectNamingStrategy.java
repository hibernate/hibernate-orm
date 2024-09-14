/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.service.ServiceRegistry;

/**
 * A naming strategy specifically for determining the implicit naming of
 * tables and sequences relating to enhanced identifier-generators.
 *
 * @author Steve Ebersole
 *
 * @since 6
 */
@Incubating
public interface ImplicitDatabaseObjectNamingStrategy {
	String DEF_SEQUENCE = "hibernate_sequence";

	/**
	 * Determine the implicit name for an identifier-generator sequence
	 *
	 * @see org.hibernate.id.enhanced.SequenceStyleGenerator
	 * @see org.hibernate.id.enhanced.SequenceStructure
	 */
	QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?,?> configValues,
			ServiceRegistry serviceRegistry);

	/**
	 * Determine the implicit name for an identifier-generator table
	 *
	 * @see org.hibernate.id.enhanced.TableGenerator
	 * @see org.hibernate.id.enhanced.TableStructure
	 */
	QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?,?> configValues,
			ServiceRegistry serviceRegistry);
}
