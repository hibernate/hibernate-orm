/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.enhanced;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
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
	 * @deprecated Avoid using this method as it can give erroneous results with respect to identifier quoting
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?,?> configValues,
			ServiceRegistry serviceRegistry);

	/**
	 * Determine the implicit name for an identifier-generator sequence
	 *
	 * @see org.hibernate.id.enhanced.SequenceStyleGenerator
	 * @see org.hibernate.id.enhanced.SequenceStructure
	 */
	default QualifiedName determineSequenceName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?,?> configValues,
			Database database) {
		return determineSequenceName( catalogName, schemaName, configValues, database.getServiceRegistry() );
	}

	/**
	 * Determine the implicit name for an identifier-generator table
	 *
	 * @see org.hibernate.id.enhanced.TableGenerator
	 * @see org.hibernate.id.enhanced.TableStructure
	 * @deprecated Avoid using this method as it can give erroneous results with respect to identifier quoting
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	QualifiedName determineTableName(
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
	default QualifiedName determineTableName(
			Identifier catalogName,
			Identifier schemaName,
			Map<?,?> configValues,
			Database database) {
		return determineTableName( catalogName, schemaName, configValues, database.getServiceRegistry() );
	}
}
