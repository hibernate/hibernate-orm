package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "\"RefpoolMembers\"")
@IdClass(RefpoolMemberId.class)
public class RefpoolMember implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "referee_id")
    private Integer refereeId;

    @Id
    @Column(name = "season_start_year")
    private Integer seasonStartYear;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id")
    private Club club;

    @OneToMany(mappedBy = "refpoolMember")
    private List<Assignment> assignments;

    public RefpoolMember()
    {
    }

    public RefpoolMember(RefpoolMember r)
    {
        this(r.getRefereeId(), r.getClubId(), r.getSeasonStartYear());
    }

    public RefpoolMember(Integer refereeId, Integer clubId, Integer seasonStartYear)
    {
        this.refereeId = Objects.requireNonNull(refereeId);
        this.seasonStartYear = Objects.requireNonNull(seasonStartYear);

        this.club = new Club(clubId);
    }

    public Integer getRefereeId()
    {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId)
    {
        this.refereeId = refereeId;
    }

    public Integer getClubId()
    {
        return club.getId();
    }

    public void setClubId(Integer clubId)
    {
        club.setId(clubId);
    }

    public Integer getSeasonStartYear()
    {
        return seasonStartYear;
    }

    public void setSeasonStartYear(Integer seasonStartYear)
    {
        this.seasonStartYear = seasonStartYear;
    }

    public Club getClub()
    {
        return club;
    }

    public void setClub(Club club)
    {
        this.club = club;
    }

    public List<Assignment> getAssignments()
    {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments)
    {
        this.assignments = assignments;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (club == null) ? 0 : club.hashCode() );
        result = prime * result + ( (refereeId == null) ? 0 : refereeId.hashCode() );
        result = prime * result + ( (seasonStartYear == null) ? 0 : seasonStartYear.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        RefpoolMember other = ( RefpoolMember ) obj;
        if ( club == null )
        {
            if ( other.club != null )
                return false;
        }
        else if ( !club.equals( other.club ) )
            return false;
        if ( refereeId == null )
        {
            if ( other.refereeId != null )
                return false;
        }
        else if ( !refereeId.equals( other.refereeId ) )
            return false;
        if ( seasonStartYear == null )
        {
            if ( other.seasonStartYear != null )
                return false;
        }
        else if ( !seasonStartYear.equals( other.seasonStartYear ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + refereeId + ", " + seasonStartYear + "]";
    }
}
