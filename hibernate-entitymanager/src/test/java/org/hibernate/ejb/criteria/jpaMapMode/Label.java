package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A translatable label for an object. I suspect that the real metamodel for translations will be
 * vastly more robust than this.
 */
public final class Label {
    private final Map<Locale, String> labelMap = new HashMap<Locale, String>();

    public Label(Map<Locale, String> labelMap) {
        if (labelMap != null) {
            this.labelMap.putAll(labelMap);
        }
    }

    public String getLabel(Locale locale) {
        return labelMap.get(locale);
    }

    public String getDefaultLabel() {
        return getLabel(Locale.getDefault());
    }

    @Override
    public String toString() {
        return labelMap.toString();
    }
}
