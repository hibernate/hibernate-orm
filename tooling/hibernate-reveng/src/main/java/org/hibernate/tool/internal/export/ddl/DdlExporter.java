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

	protected boolean format = false;

	protected String outputFileName = null;
	protected boolean haltOnError = false;

	protected boolean setupBoolProperty(String property, boolean defaultVal) {
		if (!getProperties().containsKey(property)) {
			return defaultVal;
		}
		return Boolean.parseBoolean(getProperties().getProperty(property));
	}

	protected void setupContext() {
		format = setupBoolProperty("format", format);
		outputFileName = getProperties().getProperty("outputFileName", outputFileName);
		haltOnError = setupBoolProperty("haltOnError", haltOnError);
		super.setupContext();
	}

	protected void cleanUpContext() {
		super.cleanUpContext();
	}

	protected void doStart() {
		Metadata metadata = getMetadata();
		final EnumSet<TargetType> targetTypes = EnumSet.noneOf( TargetType.class );
		if (getExportToConsole()) targetTypes.add(TargetType.STDOUT);
		if (getExportToDatabase()) targetTypes.add(TargetType.DATABASE);
		if (null != outputFileName) targetTypes.add(TargetType.SCRIPT);
		if (getSchemaUpdate()) {
			SchemaUpdate update = new SchemaUpdate();
			if(outputFileName == null && getDelimiter() == null && haltOnError && format)  {
				update.execute(targetTypes, metadata);
			}
			else {				
				if (null != outputFileName) {
					File outputFile = new File(getOutputDirectory(), outputFileName);
					update.setOutputFile(outputFile.getPath());		
					log.debug("delimiter ='"+ getDelimiter() + "'");
					update.setDelimiter(getDelimiter());
					update.setFormat(Boolean.valueOf(format));	
				}
				
				if (haltOnError) {
					update.setHaltOnError(Boolean.valueOf(haltOnError));
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
					if (haltOnError) {
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
			export.setHaltOnError(haltOnError);
			export.setFormat(format);
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


	/**
	 * Format the generated sql
	 */
	public void setFormat(boolean format) {
		this.format = format;
	}

	/**
	 * File out put name (default: empty)
	 */
	public void setOutputFileName(String fileName) {
		outputFileName = fileName;
	}

	public void setCreate(boolean create) {
		getProperties().put(CREATE_DATABASE, create);
	}

	public void setHaltonerror(boolean haltOnError) {
		this.haltOnError = haltOnError;
	}
	
	
	private String getDelimiter() {
		if (!getProperties().containsKey(DELIMITER)) {
			return ";";
		}
		return (String)getProperties().get(DELIMITER);
	}

	private boolean getExportToConsole() {
		if (!getProperties().containsKey(EXPORT_TO_CONSOLE)) {
			return true;
		} else {
			return (boolean)getProperties().get(EXPORT_TO_CONSOLE);
		}
	}
	
	private boolean getCreate() {
		if (!getProperties().containsKey(CREATE_DATABASE)) {
			return true;
		} else {
			return (boolean)getProperties().get(CREATE_DATABASE);
		}
	}
	
	private boolean getDrop() {
		if (!getProperties().containsKey(DROP_DATABASE)) {
			return false;
		} else {
			return (boolean)getProperties().get(DROP_DATABASE);
		}
	}
	
	private boolean getExportToDatabase() {
		if (!getProperties().containsKey(EXPORT_TO_DATABASE)) {
			return true;
		} else {
			return (boolean)getProperties().get(EXPORT_TO_DATABASE);
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
