package org.hibernate.tool.internal.xml;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import java.io.StringWriter;

public class DOM3LSPrettyPrinterStrategy extends AbstractXMLPrettyPrinterStrategy {
    private boolean outputComments;

    @Override
    public String prettyPrint(String xml) throws Exception {
        final Document document = newDocument(xml, "UTF-8");
        final DOMImplementationLS domImplementationLS = getDomImplementationLS(document);
        final LSSerializer lsSerializer = newLSSerializer(domImplementationLS);
        final LSOutput lsOutput = newLSOutput(domImplementationLS);

        final StringWriter stringWriter = new StringWriter();
        lsOutput.setCharacterStream(stringWriter);
        lsSerializer.write(document, lsOutput);
        return stringWriter.toString();
    }

    protected DOMImplementationLS getDomImplementationLS(final Document document) {
        final DOMImplementation domImplementation = document.getImplementation();
        if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
            return (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
        } else {
            throw new RuntimeException("DOM 3.0 LS and/or DOM 2.0 Core not supported.");
        }
    }

    protected LSSerializer newLSSerializer(final DOMImplementationLS domImplementationLS) {
        final LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
        final DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
        if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
            lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            if (domConfiguration.canSetParameter("comments", isOutputComments())) {
                lsSerializer.getDomConfig().setParameter("comments", isOutputComments());
            }
            return lsSerializer;
        } else {
            throw new RuntimeException("DOMConfiguration 'format-pretty-print' parameter isn't settable.");
        }
    }

    protected LSOutput newLSOutput(DOMImplementationLS domImplementationLS) {
        final LSOutput lsOutput = domImplementationLS.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        return lsOutput;
    }

    public boolean isOutputComments() {
        return outputComments;
    }

    public void setOutputComments(boolean outputComments) {
        this.outputComments = outputComments;
    }
}
