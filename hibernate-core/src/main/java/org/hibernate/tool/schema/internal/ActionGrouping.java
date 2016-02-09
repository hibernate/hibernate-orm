/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;

/**
 * For JPA-style schema-gen handling database and script target handing are
 * configured individually.  This tuple allows grouping the action for both
 * targets.
 *
 * @author Steve Ebersole
 */
public class ActionGrouping {
	private final Action databaseAction;
	private final Action scriptAction;

	public ActionGrouping(Action databaseAction, Action scriptAction) {
		this.databaseAction = databaseAction;
		this.scriptAction = scriptAction;
	}

	public Action getDatabaseAction() {
		return databaseAction;
	}

	public Action getScriptAction() {
		return scriptAction;
	}

	public boolean needsJdbcAccess() {
		if ( databaseAction != Action.NONE ) {
			// to execute the commands
			return true;
		}

		switch ( scriptAction ) {
			case VALIDATE:
			case UPDATE: {
				// to get the existing metadata
				return true;
			}
		}

		return false;
	}

	public static ActionGrouping interpret(Map configurationValues) {
		// interpret the JPA settings first
		Action databaseAction = Action.interpretJpaSetting(
				configurationValues.get( AvailableSettings.HBM2DDL_DATABASE_ACTION )
		);
		Action scriptAction = Action.interpretJpaSetting(
				configurationValues.get( AvailableSettings.HBM2DDL_SCRIPTS_ACTION )
		);

		// if no JPA settings were specified, look at the legacy HBM2DDL_AUTO setting...
		if ( databaseAction == Action.NONE && scriptAction == Action.NONE ) {
			final Action hbm2ddlAutoAction = Action.interpretHbm2ddlSetting(
					configurationValues.get( AvailableSettings.HBM2DDL_AUTO )
			);
			if ( hbm2ddlAutoAction != Action.NONE ) {
				databaseAction = hbm2ddlAutoAction;
			}
		}

		return new ActionGrouping( databaseAction, scriptAction );
	}
}
