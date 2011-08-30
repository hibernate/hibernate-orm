package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@MappedSuperclass
public class Building {

    public Long height;

    private Room room;

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    @ManyToOne
    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
