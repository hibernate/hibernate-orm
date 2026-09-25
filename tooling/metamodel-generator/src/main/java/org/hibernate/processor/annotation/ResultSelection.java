package org.hibernate.processor.annotation;

import java.util.List;

record ResultSelection(String resultTypeName, List<String> paths, List<String> componentNames, boolean recordProjection) {
}
