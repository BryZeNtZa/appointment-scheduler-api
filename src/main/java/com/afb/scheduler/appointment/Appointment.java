package com.afb.scheduler.appointment;

import com.afb.scheduler.department.Department;
import com.afb.scheduler.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ref_rdv", nullable = false, unique = true, updatable = false)
    private String refRdv;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "date_rdv", nullable = false)
    private LocalDateTime dateRdv;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "motif_rdv")
    private String motifRdv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "appointment_participant",
            joinColumns = @JoinColumn(name = "appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private Set<User> participants = new LinkedHashSet<>();

    protected Appointment() {
    }

    public Appointment(String refRdv, User manager, Department department,
                       LocalDateTime dateRdv, String motifRdv) {
        this.refRdv = refRdv;
        this.manager = manager;
        this.department = department;
        this.dateRdv = dateRdv;
        this.slotStart = TimeSlot.slotStartOf(dateRdv);
        this.motifRdv = motifRdv;
        this.status = AppointmentStatus.BOOKED;
    }

    public void addParticipant(User client) {
        this.participants.add(client);
    }

    public void cancel() {
        this.status = AppointmentStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public String getRefRdv() {
        return refRdv;
    }

    public User getManager() {
        return manager;
    }

    public Department getDepartment() {
        return department;
    }

    public LocalDateTime getDateRdv() {
        return dateRdv;
    }

    public LocalDateTime getSlotStart() {
        return slotStart;
    }

    public String getMotifRdv() {
        return motifRdv;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public Set<User> getParticipants() {
        return participants;
    }
}
