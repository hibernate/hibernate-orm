/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.java;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class DefaultJavaPrettyPrinterStrategy {

	public boolean formatFile(File file) {
		try {
			String source = new String( Files.readAllBytes( file.toPath() ) );
			Map<String, String> options = JavaCore.getOptions();
			CodeFormatter formatter = ToolFactory.createCodeFormatter( options );
			TextEdit edit = formatter.format(
					CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS,
					source, 0, source.length(), 0, System.lineSeparator() );
			if ( edit == null ) {
				return false;
			}
			Document document = new Document( source );
			edit.apply( document );
			Files.write( file.toPath(), document.get().getBytes() );
			return true;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

}
