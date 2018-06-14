package org.hibernate.tool.internal.export.pojo;

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
