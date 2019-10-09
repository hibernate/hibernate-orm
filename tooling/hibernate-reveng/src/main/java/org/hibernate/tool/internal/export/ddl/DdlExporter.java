/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.tool.internal.export.ddl;

import java.io.File;
import java.util.EnumSet;
import java.util.Iterator;

import org.hibernate.boot.Metadata;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaExport.Action;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.hibernate.tool.schema.TargetType;

/**
 * Schema Export (.ddl) code generation. 
 * 
 * @author Vitali
 * 
 */
public class DdlExporter extends AbstractExporter {

	protected void doStart() {
		String outputFileName = getProperties().getProperty(OUTPUT_FILE_NAME);
		Metadata metadata = getMetadata();
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf( TargetType.class );
		if (getExportToConsole()) targetTypes.add(TargetType.STDOUT);
		if (getExportToDatabase()) targetTypes.add(TargetType.DATABASE);
		if (null != outputFileName) targetTypes.add(TargetType.SCRIPT);
		if (getSchemaUpdate()) {
			SchemaUpdate update = new SchemaUpdate();
			if(outputFileName == null && getDelimiter() == null && getHaltOnError() && getFormat())  {
				update.execute(targetTypes, metadata);
			}
			else {				
				if (null != outputFileName) {
					File outputFile = new File(getOutputDirectory(), outputFileName);
					update.setOutputFile(outputFile.getPath());		
					log.debug("delimiter ='"+ getDelimiter() + "'");
					update.setDelimiter(getDelimiter());
					update.setFormat(Boolean.valueOf(getFormat()));	
				}
				
				if (getHaltOnError()) {
					update.setHaltOnError(Boolean.valueOf(getHaltOnError()));
				}
				
				update.execute(targetTypes, metadata);
				if (!update.getExceptions().isEmpty()) {
					int i = 1;
					for (Iterator<?> iterator = update.getExceptions().iterator(); iterator
							.hasNext(); i++) {
						Throwable element = (Throwable) iterator.next();
						log.warn("Error #" + i + ": ", element);

					}
					log.error(i - 1 + " errors occurred while performing Hbm2DDLExporter.");
					if (getHaltOnError()) {
						throw new RuntimeException(
								"Errors while performing Hbm2DDLExporter");
					}
				}					
			}

		} else {
			SchemaExport export = new SchemaExport();
			if (null != outputFileName) {
				export.setOutputFile(new File(getOutputDirectory(),
						outputFileName).toString());
			}
			if (null != getDelimiter()) {
				export.setDelimiter(getDelimiter());
			}
			export.setHaltOnError(getHaltOnError());
			export.setFormat(getFormat());
			if (getDrop() && getCreate()) {
				export.execute(targetTypes, Action.BOTH, metadata);
			} else if (getDrop()) {
				export.execute(targetTypes, Action.DROP, metadata);
			} else if (getCreate()) {
				export.execute(targetTypes, Action.CREATE, metadata);
			} else {
				export.execute(targetTypes, Action.NONE, metadata);
			}
		}
		
	}

	private boolean getCreate() {
		if (!getProperties().containsKey(CREATE_DATABASE)) {
			return true;
		} else {
			return (boolean)getProperties().get(CREATE_DATABASE);
		}
	}
	
	private String getDelimiter() {
		if (!getProperties().containsKey(DELIMITER)) {
			return ";";
		}
		return (String)getProperties().get(DELIMITER);
	}

	private boolean getDrop() {
		if (!getProperties().containsKey(DROP_DATABASE)) {
			return false;
		} else {
			return (boolean)getProperties().get(DROP_DATABASE);
		}
	}
	
	private boolean getExportToConsole() {
		if (!getProperties().containsKey(EXPORT_TO_CONSOLE)) {
			return true;
		} else {
			return (boolean)getProperties().get(EXPORT_TO_CONSOLE);
		}
	}
	
	private boolean getExportToDatabase() {
		if (!getProperties().containsKey(EXPORT_TO_DATABASE)) {
			return true;
		} else {
			return (boolean)getProperties().get(EXPORT_TO_DATABASE);
		}
	}
	
	private boolean getFormat() {
		if (!getProperties().containsKey(FORMAT)) {
			return false;
		} else {
			return (boolean)getProperties().get(FORMAT);
		}
	}
	
	private boolean getHaltOnError() {
		if (!getProperties().containsKey(HALT_ON_ERROR)) {
			return false;
		} else {
			return (boolean)getProperties().get(HALT_ON_ERROR);
		}
	}
	
	private boolean getSchemaUpdate() {
		if (!getProperties().containsKey(SCHEMA_UPDATE)) {
			return false;
		} else {
			return (boolean)getProperties().get(SCHEMA_UPDATE);
		}
	}
	
}
