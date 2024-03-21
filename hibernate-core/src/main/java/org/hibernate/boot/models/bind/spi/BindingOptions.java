/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.spi;

import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 */
public interface BindingOptions {
	Identifier getDefaultCatalogName();
	Identifier getDefaultSchemaName();

	EnumSet<QuotedIdentifierTarget> getGloballyQuotedIdentifierTargets();
}
