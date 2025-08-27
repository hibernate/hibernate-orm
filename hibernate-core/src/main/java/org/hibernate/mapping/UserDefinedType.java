/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.ContributableDatabaseObject;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;

/**
 * A mapping model object which represents a user defined type.
 *
 * @see UserDefinedObjectType
 * @see UserDefinedArrayType
 */
@Incubating
public interface UserDefinedType extends Serializable, ContributableDatabaseObject {

	String getQualifiedName(SqlStringGenerationContext context);

	String getName();

	Identifier getNameIdentifier();

	String getQuotedName();

	String getQuotedName(Dialect dialect);

	QualifiedTableName getQualifiedTableName();

	boolean isQuoted();

	String getSchema();

	String getQuotedSchema();

	String getQuotedSchema(Dialect dialect);

	boolean isSchemaQuoted();

	String getCatalog();

	String getQuotedCatalog();

	boolean isCatalogQuoted();
}
