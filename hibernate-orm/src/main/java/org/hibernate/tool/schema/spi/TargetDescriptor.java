/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.EnumSet;

import org.hibernate.tool.schema.TargetType;

/**
 * Describes the target(s) of schema create, drop and migrate actions.
 * <p/>
 * The purpose of this "abstraction" of a target is to enable other
 * back-ends (OGM) by simply describing where to target rather than
 * defining the targets themselves.  The reason being that ultimately
 * the Java type representing a "DDL command" sent to these targets
 * might be different (e.g., String for JDBC).
 *
 * @author Steve Ebersole
 */
public interface TargetDescriptor {
	/**
	 * The target type described here.
	 *
	 * @return The target type.
	 */
	EnumSet<TargetType> getTargetTypes();

	/**
	 * If {@link #getTargetTypes()} includes scripts, return a representation
	 * of the script file to write to.  Otherwise, returns {@code null}.
	 * <p/>
	 * While it is ultimately up to the actual tooling provider, it is generally an error
	 * for {@link #getTargetTypes()} to indicate that scripts are a target and for this
	 * method to return {@code null}.
	 *
	 * @return The script output target
	 */
	ScriptTargetOutput getScriptTargetOutput();
}
