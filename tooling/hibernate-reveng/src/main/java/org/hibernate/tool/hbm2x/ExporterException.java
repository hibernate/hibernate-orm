/*
 * Created on 02-Dec-2004
 *
 */
package org.hibernate.tool.hbm2x;

import java.io.ObjectStreamClass;

/**
 * Exception to use in Exporters.
 * @author max
 *
 */
public class ExporterException extends RuntimeException {

	private static final long serialVersionUID = 
			ObjectStreamClass.lookup(ExporterException.class).getSerialVersionUID();
	
	public ExporterException() {
		super();
	}
	public ExporterException(String message) {
		super(message);
	}
	public ExporterException(String message, Throwable cause) {
		super(message, cause);
	}
	public ExporterException(Throwable cause) {
		super(cause);
	}
}
