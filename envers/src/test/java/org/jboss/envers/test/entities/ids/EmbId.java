package org.jboss.envers.test.entities.ids;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class EmbId implements Serializable {
    private Integer x;
    private Integer y;

    public EmbId() {
    }

    public EmbId(Integer x, Integer y) {
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
        if (!(o instanceof EmbId)) return false;

        EmbId embId = (EmbId) o;

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
        return "EmbId(" + x + ", " + y + ")";
    }
}