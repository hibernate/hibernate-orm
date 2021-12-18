package org.hibernate.cfg.spi;

import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.PropertyData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface ColumnCommentParser  extends Comparable<ColumnCommentParser>{
    /**
     *
     * @param xProperty
     * @return Returns NULL if not supported or not present
     */
    String parsingColumnComment(XProperty xProperty);
}
