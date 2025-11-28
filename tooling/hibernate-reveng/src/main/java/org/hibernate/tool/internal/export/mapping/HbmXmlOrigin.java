package org.hibernate.tool.internal.export.mapping;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;

import java.io.File;
import java.io.Serial;

public class  HbmXmlOrigin extends Origin {

    @Serial
    private static final long serialVersionUID = 1L;

    private final File hbmXmlFile;

    public HbmXmlOrigin(File hbmXmlFile) {
        super( SourceType.FILE, hbmXmlFile.getAbsolutePath() );
        this.hbmXmlFile = hbmXmlFile;
    }

    public File getHbmXmlFile() {
        return hbmXmlFile;
    }

}
