package org.hibernate.tool.api.xml;

public interface XMLPrettyPrinterStrategy {

    String prettyPrint(String xml) throws Exception;
}
