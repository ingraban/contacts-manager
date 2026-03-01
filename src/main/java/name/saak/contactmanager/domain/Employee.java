package name.saak.contactmanager.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mandatory OneToOne to Contact
    @NotNull(message = "Contact ist erforderlich")
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false, unique = true)
    private Contact contact;

    // Mandatory ManyToOne to Department
    @NotNull(message = "Department ist erforderlich")
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Weekly hours (mandatory)
    @NotNull(message = "Wochenstunden sind erforderlich")
    @Min(value = 1, message = "Wochenstunden müssen mindestens 1 sein")
    @Column(name = "weekly_hours", nullable = false)
    private Integer weeklyHours;

    // Working days (7 Boolean fields)
    @Column(name = "works_monday", nullable = false)
    private Boolean worksMonday = false;

    @Column(name = "works_tuesday", nullable = false)
    private Boolean worksTuesday = false;

    @Column(name = "works_wednesday", nullable = false)
    private Boolean worksWednesday = false;

    @Column(name = "works_thursday", nullable = false)
    private Boolean worksThursday = false;

    @Column(name = "works_friday", nullable = false)
    private Boolean worksFriday = false;

    @Column(name = "works_saturday", nullable = false)
    private Boolean worksSaturday = false;

    @Column(name = "works_sunday", nullable = false)
    private Boolean worksSunday = false;

    // Daily work hours (7 LocalTime fields, nullable)
    @Column(name = "monday_hours")
    private LocalTime mondayHours;

    @Column(name = "tuesday_hours")
    private LocalTime tuesdayHours;

    @Column(name = "wednesday_hours")
    private LocalTime wednesdayHours;

    @Column(name = "thursday_hours")
    private LocalTime thursdayHours;

    @Column(name = "friday_hours")
    private LocalTime fridayHours;

    @Column(name = "saturday_hours")
    private LocalTime saturdayHours;

    @Column(name = "sunday_hours")
    private LocalTime sundayHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        initializeBooleanFields();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        initializeBooleanFields();
        updatedAt = LocalDateTime.now();
    }

    // Initialize Boolean fields to false if null
    private void initializeBooleanFields() {
        if (worksMonday == null) worksMonday = false;
        if (worksTuesday == null) worksTuesday = false;
        if (worksWednesday == null) worksWednesday = false;
        if (worksThursday == null) worksThursday = false;
        if (worksFriday == null) worksFriday = false;
        if (worksSaturday == null) worksSaturday = false;
        if (worksSunday == null) worksSunday = false;
    }

    // Constructors
    public Employee() {
    }

    public Employee(Contact contact, Department department, Integer weeklyHours) {
        this.contact = contact;
        this.department = department;
        this.weeklyHours = weeklyHours;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Integer getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public Boolean getWorksMonday() {
        return worksMonday;
    }

    public void setWorksMonday(Boolean worksMonday) {
        this.worksMonday = worksMonday;
    }

    public Boolean getWorksTuesday() {
        return worksTuesday;
    }

    public void setWorksTuesday(Boolean worksTuesday) {
        this.worksTuesday = worksTuesday;
    }

    public Boolean getWorksWednesday() {
        return worksWednesday;
    }

    public void setWorksWednesday(Boolean worksWednesday) {
        this.worksWednesday = worksWednesday;
    }

    public Boolean getWorksThursday() {
        return worksThursday;
    }

    public void setWorksThursday(Boolean worksThursday) {
        this.worksThursday = worksThursday;
    }

    public Boolean getWorksFriday() {
        return worksFriday;
    }

    public void setWorksFriday(Boolean worksFriday) {
        this.worksFriday = worksFriday;
    }

    public Boolean getWorksSaturday() {
        return worksSaturday;
    }

    public void setWorksSaturday(Boolean worksSaturday) {
        this.worksSaturday = worksSaturday;
    }

    public Boolean getWorksSunday() {
        return worksSunday;
    }

    public void setWorksSunday(Boolean worksSunday) {
        this.worksSunday = worksSunday;
    }

    public LocalTime getMondayHours() {
        return mondayHours;
    }

    public void setMondayHours(LocalTime mondayHours) {
        this.mondayHours = mondayHours;
    }

    public LocalTime getTuesdayHours() {
        return tuesdayHours;
    }

    public void setTuesdayHours(LocalTime tuesdayHours) {
        this.tuesdayHours = tuesdayHours;
    }

    public LocalTime getWednesdayHours() {
        return wednesdayHours;
    }

    public void setWednesdayHours(LocalTime wednesdayHours) {
        this.wednesdayHours = wednesdayHours;
    }

    public LocalTime getThursdayHours() {
        return thursdayHours;
    }

    public void setThursdayHours(LocalTime thursdayHours) {
        this.thursdayHours = thursdayHours;
    }

    public LocalTime getFridayHours() {
        return fridayHours;
    }

    public void setFridayHours(LocalTime fridayHours) {
        this.fridayHours = fridayHours;
    }

    public LocalTime getSaturdayHours() {
        return saturdayHours;
    }

    public void setSaturdayHours(LocalTime saturdayHours) {
        this.saturdayHours = saturdayHours;
    }

    public LocalTime getSundayHours() {
        return sundayHours;
    }

    public void setSundayHours(LocalTime sundayHours) {
        this.sundayHours = sundayHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // equals and hashCode based on contact (business key)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(contact, employee.contact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contact);
    }

    @Override
    public String toString() {
        return "Employee{" +
               "id=" + id +
               ", contact=" + (contact != null ? contact.getId() : null) +
               ", department=" + (department != null ? department.getName() : null) +
               ", weeklyHours=" + weeklyHours +
               '}';
    }
}
