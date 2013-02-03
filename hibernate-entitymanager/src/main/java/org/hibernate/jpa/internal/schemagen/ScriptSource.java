/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.internal.schemagen;

import javax.persistence.PersistenceException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.jboss.logging.Logger;

import org.hibernate.tool.hbm2ddl.ImportSqlCommandExtractor;

/**
 * @author Steve Ebersole
 */
public class ScriptSource implements GenerationSource {
	private static final Logger log = Logger.getLogger( ScriptSource.class );

	private final SqlScriptReader createSource;
	private final SqlScriptReader dropSource;
	private final ImportSqlCommandExtractor scriptCommandExtractor;

	public ScriptSource(
			Object createScriptSourceSetting,
			Object dropScriptSourceSetting,
			ImportSqlCommandExtractor scriptCommandExtractor) {
		this.scriptCommandExtractor = scriptCommandExtractor;

		if ( Reader.class.isInstance( createScriptSourceSetting ) ) {
			createSource = new ReaderScriptSource( (Reader) createScriptSourceSetting );
		}
		else {
			createSource = new FileScriptSource( createScriptSourceSetting.toString() );
		}

		if ( Writer.class.isInstance( dropScriptSourceSetting ) ) {
			dropSource = new ReaderScriptSource( (Reader) dropScriptSourceSetting );
		}
		else {
			dropSource = new FileScriptSource( dropScriptSourceSetting.toString() );
		}
	}

	@Override
	public Iterable<String> getCreateCommands() {
		return createSource.read( scriptCommandExtractor );
	}

	@Override
	public Iterable<String> getDropCommands() {
		return dropSource.read( scriptCommandExtractor );
	}

	@Override
	public void release() {
		createSource.release();
		dropSource.release();
	}

}
