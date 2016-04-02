package org.hibernate.envers.test.integration.ids.idclass;

import java.io.Serializable;

/**
 * Created by yinzara on 4/1/16.
 */
public class ReferenceIdentifierClassId implements Serializable {
    private Integer iiie;
    private String type;

    public ReferenceIdentifierClassId() {
    }

    public ReferenceIdentifierClassId(Integer iiie, String type) {
        this.iiie = iiie;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if ( this == o ) {
            return true;
        }
        if ( !(o instanceof ReferenceIdentifierClassId) ) {
            return false;
        }

        ReferenceIdentifierClassId that = (ReferenceIdentifierClassId) o;

        if ( iiie != null ? !iiie.equals( that.iiie) : that.iiie != null ) {
            return false;
        }
        if ( type != null ? !type.equals( that.type ) : that.type != null ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = iiie != null ? iiie.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ReferenceIdentifierClassId(iiie = " + iiie + ", type = " + type + ")";
    }

    public Integer getIiie() {
        return iiie;
    }

    public void setIiie(Integer iiie) {
        this.iiie = iiie;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
