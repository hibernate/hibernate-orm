package org.jboss.envers.test.integration.naming.ids;

import org.jboss.envers.test.entities.ids.EmbId;

import javax.persistence.Embeddable;
import javax.persistence.Column;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class EmbIdNaming implements Serializable {
    @Column(name = "XX")
    private Integer x;

    @Column(name = "YY")
    private Integer y;

    public EmbIdNaming() {
    }

    public EmbIdNaming(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmbIdNaming)) return false;

        EmbIdNaming embId = (EmbIdNaming) o;

        if (x != null ? !x.equals(embId.x) : embId.x != null) return false;
        if (y != null ? !y.equals(embId.y) : embId.y != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (x != null ? x.hashCode() : 0);
        result = 31 * result + (y != null ? y.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "EmbIdNaming(" + x + ", " + y + ")";
    }
}