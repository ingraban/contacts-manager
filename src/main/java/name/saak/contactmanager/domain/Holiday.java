package name.saak.contactmanager.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "holiday")
public class Holiday {

    @Id
    @NotNull(message = "Datum ist erforderlich")
    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @NotBlank(message = "Name ist erforderlich")
    @Size(max = 50, message = "Name darf maximal 50 Zeichen lang sein")
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Size(max = 500, message = "Beschreibung darf maximal 500 Zeichen lang sein")
    @Column(name = "beschreibung", length = 500)
    private String beschreibung;

    @NotNull(message = "Frei ist erforderlich")
    @Column(name = "frei", nullable = false)
    private Boolean frei = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (frei == null) {
            frei = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (frei == null) {
            frei = true;
        }
    }

    // Constructors
    public Holiday() {
    }

    public Holiday(LocalDate date, String name, Boolean frei) {
        this.date = date;
        this.name = name;
        this.frei = frei;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public Boolean getFrei() {
        return frei;
    }

    public void setFrei(Boolean frei) {
        this.frei = frei;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // equals and hashCode based on date (business key)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Holiday holiday = (Holiday) o;
        return Objects.equals(date, holiday.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return "Holiday{" +
               "date=" + date +
               ", name='" + name + '\'' +
               ", frei=" + frei +
               '}';
    }
}
