package org.hibernate.tool.internal.util;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

public class DummyDialect extends Dialect {
    static final DummyDialect INSTANCE = new DummyDialect();
    public DummyDialect() {
        super(new DatabaseVersion() {
            @Override
            public int getDatabaseMajorVersion() {
                return 0;
            }
            @Override
            public int getDatabaseMinorVersion() {
                return 0;
            }
        });
    }


}
