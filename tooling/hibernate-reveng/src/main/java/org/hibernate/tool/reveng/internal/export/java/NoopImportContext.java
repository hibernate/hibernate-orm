package org.hibernate.tool.reveng.internal.export.java;

public class NoopImportContext implements ImportContext {

	public String importType(String fqcn) {
		return fqcn;
	}

	public String staticImport(String fqcn, String member) {
		return fqcn;
	}

	public String generateImports() {
		return "";
	}

}
