package org.hibernate.test.bytecode.enhancement.persist.HHH_14553;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "\"Ships\"")
public class Ship
{
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Basic(optional = false)
    @Column
    private String name;

//    @Basic(fetch = FetchType.LAZY)
//    @Column
//    @Lob
//    private byte[] thumbnail;
//
//    @Basic(fetch = FetchType.LAZY)
//    @Column(name = "full_image")
//    @Lob
//    private byte[] fullImage;

    public Ship()
    {
    }

    public Ship(String name)
    {
        this(null, name);
    }

    public Ship(Long id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

//    public byte[] getThumbnail()
//    {
//        return thumbnail;
//    }
//
//    public void setThumbnail(byte[] thumbnail)
//    {
//        this.thumbnail = thumbnail;
//    }
//
//    public byte[] getFullImage()
//    {
//        return fullImage;
//    }
//
//    public void setFullImage(byte[] fullImage)
//    {
//        this.fullImage = fullImage;
//    }
}
